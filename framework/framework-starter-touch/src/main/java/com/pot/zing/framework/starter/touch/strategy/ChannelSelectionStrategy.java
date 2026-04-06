package com.pot.zing.framework.starter.touch.strategy;

import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import com.pot.zing.framework.starter.touch.model.TouchRequest;

import java.util.List;

/**
 * Strategy for choosing primary and fallback touch channels.
 */
public interface ChannelSelectionStrategy {

    /**
     * Selects the primary channel.
     */
    TouchChannelType selectChannel(TouchRequest request);

    /**
     * Selects fallback channels.
     */
    List<TouchChannelType> selectFallbackChannels(TouchRequest request);
}
