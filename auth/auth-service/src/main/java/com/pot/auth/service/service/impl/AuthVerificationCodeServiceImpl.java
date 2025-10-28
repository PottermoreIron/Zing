package com.pot.auth.service.service.impl;

import com.pot.auth.service.enums.VerificationBizType;
import com.pot.auth.service.service.AuthVerificationCodeService;
import com.pot.auth.service.service.adapter.VerificationCodeAdapter;
import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.util.ValidationUtils;
import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/10/19 18:09
 * @description: 认证验证码服务接口实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthVerificationCodeServiceImpl implements AuthVerificationCodeService {

    private final VerificationCodeAdapter verificationCodeAdapter;

    @Override
    public void sendRegisterCode(String target, TouchChannelType type) {
        validateTarget(target, type);
        verificationCodeAdapter.sendCode(target, type, VerificationBizType.REGISTER.getCode());
    }

    @Override
    public void sendLoginCode(String target, TouchChannelType type) {
        validateTarget(target, type);
        verificationCodeAdapter.sendCode(target, type, VerificationBizType.LOGIN.getCode());
    }

    @Override
    public void sendResetPasswordCode(String target, TouchChannelType type) {
        validateTarget(target, type);
        verificationCodeAdapter.sendCode(target, type, VerificationBizType.RESET_PASSWORD.getCode());
    }

    private void validateTarget(String target, TouchChannelType type) {
        switch (type) {
            case EMAIL -> {
                if (!ValidationUtils.isEmail(target)) {
                    throw new BusinessException(ResultCode.EMAIL_NOT_LEGAL);
                }
            }
            case SMS -> {
                if (!ValidationUtils.isPhone(target)) {
                    throw new BusinessException(ResultCode.PHONE_NOT_LEGAL);
                }
            }
        }
    }
}
