package com.pot.zing.framework.starter.touch.service.impl;

import com.pot.zing.framework.common.util.ValidationUtils;
import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import com.pot.zing.framework.starter.touch.exception.TouchException;
import com.pot.zing.framework.starter.touch.model.TouchRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public abstract class AbstractSmsChannelImpl extends AbstractTouchChannelImpl {

    @Override
    public TouchChannelType getChannelType() {
        return TouchChannelType.SMS;
    }

    @Override
    protected void validateRequest(TouchRequest request) {
        if (!ValidationUtils.isValidPhone(request.getTarget())) {
            throw new TouchException("Invalid phone number format: " + request.getTarget());
        }

        if (request.getTemplateId() == null || request.getTemplateId().trim().isEmpty()) {
            throw new TouchException("SMS template ID must not be blank");
        }
    }

    @Override
    protected String doSend(TouchRequest request) {
        return sendSms(
                request.getTarget(),
                request.getTemplateId(),
                request.getParams());
    }

    /**
     * Sends the SMS through the concrete provider.
     */
    protected abstract String sendSms(String phone, String templateId, Map<String, Object> params);
}