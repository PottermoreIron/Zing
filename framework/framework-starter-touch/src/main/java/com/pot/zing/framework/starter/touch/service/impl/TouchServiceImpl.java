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
 * @author: Pot
 * @created: 2025/10/19 15:37
 * @description: 触达服务
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
        // 注册渠道并按优先级排序
        channels.stream()
                .sorted(Comparator.comparingInt(TouchChannel::getPriority))
                .forEach(channel -> channelMap.put(channel.getChannelType(), channel));

        log.info("触达渠道初始化完成: {}", channelMap.keySet());
    }

    /**
     * 发送消息
     */
    public R<TouchResponse> send(TouchRequest request) {
        // 参数校验
        validateRequest(request);

        // 选择渠道
        TouchChannelType channelType = request.getChannelType() != null
                ? request.getChannelType()
                : selectionStrategy.selectChannel(request);

        // 获取渠道实现
        TouchChannel channel = getChannel(channelType);

        // 检查渠道可用性
        if (!channel.isAvailable()) {
            log.warn("渠道不可用: {}", channelType);
            return R.fail("渠道暂不可用");
        }

        // 发送消息
        return channel.send(request);
    }

    /**
     * 多渠道发送(带降级策略)
     */
    public R<TouchResponse> sendWithFallback(TouchRequest request) {
        R<TouchResponse> result = send(request);

        // 主渠道失败,尝试降级
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
                            log.info("降级发送成功: channel={}", fallback);
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * 批量发送
     */
    public R<List<TouchResponse>> batchSend(List<TouchRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return R.fail("请求列表不能为空");
        }

        // 按渠道分组
        Map<TouchChannelType, List<TouchRequest>> groupedRequests = new ConcurrentHashMap<>();
        requests.forEach(req -> {
            TouchChannelType type = req.getChannelType() != null
                    ? req.getChannelType()
                    : selectionStrategy.selectChannel(req);
            groupedRequests.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(req);
        });

        // 按渠道批量发送
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
            throw new TouchException("请求对象不能为空");
        }
        if (request.getTarget() == null || request.getTarget().trim().isEmpty()) {
            throw new TouchException("接收目标不能为空");
        }
    }

    private TouchChannel getChannel(TouchChannelType type) {
        return Optional.ofNullable(channelMap.get(type))
                .orElseThrow(() -> new TouchException("不支持的触达渠道: " + type));
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
