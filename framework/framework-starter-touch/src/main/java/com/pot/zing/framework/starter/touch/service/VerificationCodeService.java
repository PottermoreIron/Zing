package com.pot.zing.framework.starter.touch.service;

import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.touch.model.VerificationCodeRequest;
import com.pot.zing.framework.starter.touch.model.VerificationCodeResponse;

/**
 * @author: Pot
 * @created: 2025/10/19 16:49
 * @description: 验证码接口类
 */
public interface VerificationCodeService {
    /**
     * 发送验证码
     */
    R<VerificationCodeResponse> sendCode(VerificationCodeRequest request);

    /**
     * 验证验证码
     */
    R<Boolean> validateCode(String target, String code, String bizType);

    /**
     * 删除验证码
     */
    void deleteCode(String target, String bizType);
}
