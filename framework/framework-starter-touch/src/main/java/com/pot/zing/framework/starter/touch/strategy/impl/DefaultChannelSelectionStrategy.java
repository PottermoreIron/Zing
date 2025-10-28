package com.pot.zing.framework.starter.touch.strategy.impl;

import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import com.pot.zing.framework.starter.touch.model.TouchRequest;
import com.pot.zing.framework.starter.touch.strategy.ChannelSelectionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Pot
 * @created: 2025/10/19 16:12
 * @description: 默认渠道选择策略
 */
@Slf4j
@Component
public class DefaultChannelSelectionStrategy implements ChannelSelectionStrategy {

    @Override
    public TouchChannelType selectChannel(TouchRequest request) {
        // 使用请求中指定的渠道
        return request.getChannelType();
    }

    @Override
    public List<TouchChannelType> selectFallbackChannels(TouchRequest request) {
        List<TouchChannelType> fallbacks = new ArrayList<>();

        // 根据主渠道选择降级方案
        switch (request.getChannelType()) {
            case SMS:
                fallbacks.add(TouchChannelType.EMAIL);
                break;
            case EMAIL:
                fallbacks.add(TouchChannelType.SMS);
                break;
            case PUSH:
                fallbacks.add(TouchChannelType.WEBSOCKET);
                fallbacks.add(TouchChannelType.SMS);
                break;
            default:
                break;
        }

        return fallbacks;
    }
}
