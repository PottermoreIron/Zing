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
        if (!ValidationUtils.isEmail(request.getTarget())) {
            throw new TouchException("邮箱格式错误: " + request.getTarget());
        }
    }

    @Override
    protected String doSend(TouchRequest request) {
        // 构建邮件内容
        EmailContent content = buildEmailContent(request);

        // 发送邮件
        return sendEmail(
                request.getTarget(),
                content.getSubject(),
                content.getBody(),
                content.isHtml()
        );
    }

    /**
     * 构建邮件内容(支持模板渲染)
     */
    protected abstract EmailContent buildEmailContent(TouchRequest request);

    /**
     * 子类实现具体的邮件发送逻辑
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