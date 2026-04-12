package com.pot.zing.framework.starter.touch.service.impl;

import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import com.pot.zing.framework.starter.touch.exception.TouchException;
import com.pot.zing.framework.starter.touch.model.TouchRequest;
import com.pot.zing.framework.starter.touch.model.TouchResponse;
import com.pot.zing.framework.starter.touch.service.TouchChannel;
import com.pot.zing.framework.starter.touch.service.TouchService;
import com.pot.zing.framework.starter.touch.strategy.ChannelSelectionStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default touch service implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TouchServiceImpl implements TouchService {

    private final List<TouchChannel> channels;
    private final ChannelSelectionStrategy selectionStrategy;
    private final Map<TouchChannelType, TouchChannel> channelMap = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        channels.stream()
                .sorted(Comparator.comparingInt(TouchChannel::getPriority))
                .forEach(channel -> channelMap.put(channel.getChannelType(), channel));

        log.info("[Touch] Channel initialization complete: {}", channelMap.keySet());
    }

    public R<TouchResponse> send(TouchRequest request) {
        validateRequest(request);

        TouchChannelType channelType = request.getChannelType() != null
                ? request.getChannelType()
                : selectionStrategy.selectChannel(request);

        TouchChannel channel = getChannel(channelType);

        if (!channel.isAvailable()) {
            log.warn("[Touch] Channel unavailable: {}", channelType);
            return R.fail("Channel is currently unavailable");
        }

        return channel.send(request);
    }

    public R<TouchResponse> sendWithFallback(TouchRequest request) {
        R<TouchResponse> result = send(request);

        if (!result.isSuccess()) {
            List<TouchChannelType> fallbackChannels = selectionStrategy.selectFallbackChannels(request);

            if (!fallbackChannels.isEmpty()) {
                log.warn("主渠道发送失败,尝试降级: {} -> {}",
                        request.getChannelType(), fallbackChannels);

                for (TouchChannelType fallback : fallbackChannels) {
                    TouchChannel channel = channelMap.get(fallback);
                    if (channel != null && channel.isAvailable()) {
                        TouchRequest fallbackRequest = cloneRequest(request, fallback);
                        result = channel.send(fallbackRequest);
                        if (result.isSuccess()) {
                            log.info("[Touch] Fallback delivery succeeded — channel={}", fallback);
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    public R<List<TouchResponse>> batchSend(List<TouchRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return R.fail("Request list must not be empty");
        }

        Map<TouchChannelType, List<TouchRequest>> groupedRequests = new ConcurrentHashMap<>();
        requests.forEach(req -> {
            TouchChannelType type = req.getChannelType() != null
                    ? req.getChannelType()
                    : selectionStrategy.selectChannel(req);
            groupedRequests.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(req);
        });

        List<TouchResponse> allResponses = new java.util.ArrayList<>();
        groupedRequests.forEach((type, reqs) -> {
            TouchChannel channel = channelMap.get(type);
            if (channel != null && channel.isAvailable()) {
                R<List<TouchResponse>> result = channel.batchSend(reqs);
                if (result.isSuccess() && result.getData() != null) {
                    allResponses.addAll(result.getData());
                }
            }
        });

        return R.success(allResponses);
    }

    private void validateRequest(TouchRequest request) {
        if (request == null) {
            throw new TouchException("Request object must not be null");
        }
        if (request.getTarget() == null || request.getTarget().trim().isEmpty()) {
            throw new TouchException("Delivery target must not be blank");
        }
    }

    private TouchChannel getChannel(TouchChannelType type) {
        return Optional.ofNullable(channelMap.get(type))
                .orElseThrow(() -> new TouchException("Unsupported touch channel: " + type));
    }

    private TouchRequest cloneRequest(TouchRequest original, TouchChannelType newChannelType) {
        return TouchRequest.builder()
                .target(original.getTarget())
                .channelType(newChannelType)
                .templateId(original.getTemplateId())
                .params(original.getParams())
                .bizType(original.getBizType())
                .build();
    }
}
