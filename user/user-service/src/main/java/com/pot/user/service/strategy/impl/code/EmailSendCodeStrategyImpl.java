package com.pot.user.service.strategy.impl.code;

import com.pot.common.enums.ResultCode;
import com.pot.common.exception.BusinessException;
import com.pot.common.redis.RedisService;
import com.pot.common.utils.ValidationUtils;
import com.pot.user.service.controller.request.SendCodeRequest;
import com.pot.user.service.controller.request.SendEmailCodeRequest;
import com.pot.user.service.enums.SendCodeChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/3/28 22:51
 * @description: 发送邮件验证码策略实现
 */
@Service
@Slf4j
public class EmailSendCodeStrategyImpl extends AbstractSendCodeStrategyImpl {
    public EmailSendCodeStrategyImpl(RedisService redisService) {
        super(redisService);
    }

    @Override
    protected void doSend(String target, String code) {
        // todo 发送验证码
        // 这里可以调用邮件服务发送验证码
        System.out.println("Sending Email code: " + code + " to " + target);
    }

    @Override
    protected void checkTarget(String email) {
        if (!ValidationUtils.isEmail(email)) {
            throw new BusinessException(ResultCode.EMAIL_NOT_LEGAL);
        }
    }

    @Override
    protected void setTarget(SendCodeRequest request) {
        this.target = ((SendEmailCodeRequest) request).getEmail();
    }

    @Override
    public SendCodeChannelEnum getVerificationCodeType() {
        return SendCodeChannelEnum.EMAIL;
    }
}
