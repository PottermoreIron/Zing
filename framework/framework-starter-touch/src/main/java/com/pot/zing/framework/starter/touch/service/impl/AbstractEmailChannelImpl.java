package com.pot.zing.framework.starter.touch.service.impl;

import com.pot.zing.framework.common.util.ValidationUtils;
import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import com.pot.zing.framework.starter.touch.exception.TouchException;
import com.pot.zing.framework.starter.touch.model.TouchRequest;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractEmailChannelImpl extends AbstractTouchChannelImpl {

    @Override
    public TouchChannelType getChannelType() {
        return TouchChannelType.EMAIL;
    }

    @Override
    protected void validateRequest(TouchRequest request) {
        if (!ValidationUtils.isValidEmail(request.getTarget())) {
            throw new TouchException("Invalid email address format: " + request.getTarget());
        }
    }

    @Override
    protected String doSend(TouchRequest request) {
        EmailContent content = buildEmailContent(request);

        return sendEmail(
                request.getTarget(),
                content.getSubject(),
                content.getBody(),
                content.isHtml());
    }

    /**
     * Builds email content, optionally using template rendering.
     */
    protected abstract EmailContent buildEmailContent(TouchRequest request);

    /**
     * Sends the rendered email through the concrete provider.
     */
    protected abstract String sendEmail(String email, String subject, String body, boolean isHtml);

    @Data
    @Builder
    protected static class EmailContent {
        private String subject;
        private String body;
        private boolean isHtml;
    }
}