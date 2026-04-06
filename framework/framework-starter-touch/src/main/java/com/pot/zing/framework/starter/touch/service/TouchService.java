package com.pot.zing.framework.starter.touch.service;

import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.touch.model.TouchRequest;
import com.pot.zing.framework.starter.touch.model.TouchResponse;

import java.util.List;

/**
 * Service for sending touch messages.
 */
public interface TouchService {

    /**
     * Sends a message through the selected channel.
     */
    R<TouchResponse> send(TouchRequest request);

    /**
     * Sends a message with fallback channels.
     */
    R<TouchResponse> sendWithFallback(TouchRequest request);

    /**
     * Sends multiple requests.
     */
    R<List<TouchResponse>> batchSend(List<TouchRequest> requests);
}
