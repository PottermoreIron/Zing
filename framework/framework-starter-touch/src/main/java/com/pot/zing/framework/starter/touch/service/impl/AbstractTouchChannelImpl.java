package com.pot.zing.framework.starter.touch.service.impl;

import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.touch.exception.TouchException;
import com.pot.zing.framework.starter.touch.model.TouchRequest;
import com.pot.zing.framework.starter.touch.model.TouchResponse;
import com.pot.zing.framework.starter.touch.service.TouchChannel;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Template base class for touch channels.
 */
@Slf4j
public abstract class AbstractTouchChannelImpl implements TouchChannel {

    @Override
    public R<TouchResponse> send(TouchRequest request) {
        try {
            preValidate(request);

            validateRequest(request);

            checkRateLimit(request);

            String messageId = doSend(request);

            TouchResponse response = buildResponse(messageId, request);

            postProcess(request, response);

            return R.success(response);

        } catch (TouchException e) {
            log.error("[Touch] Message delivery failed — channel={}, target={}, error={}",
                    getChannelType(), request.getTarget(), e.getMessage());
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("[Touch] Message delivery error — channel={}, target={}",
                    getChannelType(), request.getTarget(), e);
            return R.fail("Message delivery failed: " + e.getMessage());
        }
    }

    /**
     * Optional hook for preliminary validation.
     */
    protected void preValidate(TouchRequest request) {
    }

    /**
     * Validates the request.
     */
    protected abstract void validateRequest(TouchRequest request);

    /**
     * Optional hook for rate-limit checks.
     */
    protected void checkRateLimit(TouchRequest request) {
    }

    /**
     * Performs the actual send and returns a provider message ID.
     */
    protected abstract String doSend(TouchRequest request);

    /**
     * Builds the standard touch response.
     */
    protected TouchResponse buildResponse(String messageId, TouchRequest request) {
        return TouchResponse.builder()
                .messageId(messageId)
                .sendTime(LocalDateTime.now())
                .channelType(getChannelType().getCode())
                .build();
    }

    /**
     * Optional hook for audit or monitoring side effects.
     */
    protected void postProcess(TouchRequest request, TouchResponse response) {
    }
}
