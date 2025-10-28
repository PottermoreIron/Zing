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
        if (!ValidationUtils.isPhone(request.getTarget())) {
            throw new TouchException("手机号格式错误: " + request.getTarget());
        }

        if (request.getTemplateId() == null || request.getTemplateId().trim().isEmpty()) {
            throw new TouchException("短信模板ID不能为空");
        }
    }

    @Override
    protected String doSend(TouchRequest request) {
        return sendSms(
                request.getTarget(),
                request.getTemplateId(),
                request.getParams()
        );
    }

    /**
     * 子类实现具体的短信发送逻辑
     */
    protected abstract String sendSms(String phone, String templateId, Map<String, Object> params);
}