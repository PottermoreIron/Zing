package com.pot.auth.service.service.adapter;

import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import com.pot.zing.framework.starter.touch.model.VerificationCodeRequest;
import com.pot.zing.framework.starter.touch.model.VerificationCodeResponse;
import com.pot.zing.framework.starter.touch.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/10/19 17:53
 * @description: 验证码服务适配器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeAdapter {

    private final VerificationCodeService verificationCodeService;

    /**
     * 发送验证码
     */
    public void sendCode(String target, TouchChannelType type, String bizType) {
        VerificationCodeRequest request = VerificationCodeRequest.builder()
                .target(target)
                .channelType(type)
                .bizType(bizType)
                .codeLength(6)
                .expireSeconds(300L)
                .build();

        R<VerificationCodeResponse> result = verificationCodeService.sendCode(request);

        if (!result.isSuccess()) {
            log.error("验证码发送失败: target={}, type={}, error={}",
                    target, type, result.getMsg());
            throw new BusinessException(ResultCode.VERIFICATION_CODE_SEND_FAILED);
        }

        log.info("验证码发送成功: target={}, messageId={}",
                target, result.getData().getMessageId());
    }

    /**
     * 验证验证码
     */
    public void validateCode(String target, String code, String bizType) {
        R<Boolean> result = verificationCodeService.validateCode(target, code, bizType);

        if (!result.isSuccess() || !Boolean.TRUE.equals(result.getData())) {
            log.warn("验证码校验失败: target={}, bizType={}", target, bizType);
            throw new BusinessException(ResultCode.VERIFICATION_CODE_ERROR);
        }

        log.info("验证码校验成功: target={}, bizType={}", target, bizType);
    }

    /**
     * 删除验证码
     */
    public void deleteCode(String target, String bizType) {
        verificationCodeService.deleteCode(target, bizType);
        log.debug("删除验证码: target={}, bizType={}", target, bizType);
    }

    /**
     * 验证并删除验证码（用于一次性验证场景）
     *
     * @param target  目标地址（手机号/邮箱）
     * @param code    验证码
     * @param bizType 业务类型
     * @return true-验证成功，false-验证失败
     */
    public boolean verifyAndDelete(String target, String code, com.pot.auth.service.enums.VerificationBizType bizType) {
        try {
            // 1. 验证验证码
            validateCode(target, code, bizType.getCode());

            // 2. 验证成功后删除验证码（防止重复使用）
            deleteCode(target, bizType.getCode());

            return true;
        } catch (BusinessException e) {
            log.warn("验证码验证失败: target={}, bizType={}, error={}",
                    target, bizType.getCode(), e.getMessage());
            return false;
        }
    }
}
