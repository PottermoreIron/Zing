package com.pot.auth.service.service.v1;


import com.pot.zing.framework.starter.touch.model.VerificationCodeResponse;

/**
 * @author: Pot
 * @created: 2025/10/25 23:43
 * @description: 凭证服务接口
 */
public interface CredentialService {
    VerificationCodeResponse sendVerificationCode(String type, String recipient, String purpose);

    boolean verifyCode(String type, String recipient, String code);

    void changePassword(String oldPassword, String newPassword);

    void resetPassword(String credential, String code, String newPassword);
}
