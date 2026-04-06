package com.pot.zing.framework.starter.touch.service;

import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import com.pot.zing.framework.starter.touch.model.TouchRequest;
import com.pot.zing.framework.starter.touch.model.TouchResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel-specific touch delivery contract.
 */
public interface TouchChannel {

    /**
     * Returns the channel type.
     */
    TouchChannelType getChannelType();

    /**
     * Sends a request through this channel.
     */
    R<TouchResponse> send(TouchRequest request);

    /**
     * Sends multiple requests. The default implementation is sequential.
     */
    default R<List<TouchResponse>> batchSend(List<TouchRequest> requests) {
        List<TouchResponse> responses = requests.stream()
                .map(this::send)
                .filter(R::isSuccess)
                .map(R::getData)
                .collect(Collectors.toList());

        return R.success(responses);
    }

    /**
     * Returns whether the channel is currently available.
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Returns the channel priority. Lower values run first.
     */
    default int getPriority() {
        return 100;
    }
}
