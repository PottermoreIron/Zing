package com.pot.zing.framework.starter.touch.strategy;

import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import com.pot.zing.framework.starter.touch.model.TouchRequest;

import java.util.List;

/**
 * @author: Pot
 * @created: 2025/10/19 16:12
 * @description: 渠道选择策略
 */
public interface ChannelSelectionStrategy {

    /**
     * 选择发送渠道
     */
    TouchChannelType selectChannel(TouchRequest request);

    /**
     * 选择降级渠道列表
     */
    List<TouchChannelType> selectFallbackChannels(TouchRequest request);
}
