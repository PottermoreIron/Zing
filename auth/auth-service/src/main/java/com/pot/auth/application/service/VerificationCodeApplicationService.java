package com.pot.auth.application.service;

import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.Phone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service for sending and verifying verification codes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeApplicationService {

    private final VerificationCodeService verificationCodeService;

    /**
     * Sends an email verification code.
     */
    public boolean sendEmailCode(String email) {
        log.info("[应用服务] 发送邮件验证码: email={}", email);

        Email emailObj = new Email(email);
        return verificationCodeService.sendEmailVerificationCode(emailObj);
    }

    /**
     * Sends an SMS verification code.
     */
    public boolean sendSmsCode(String phoneNumber) {
        log.info("[应用服务] 发送短信验证码: phone={}", phoneNumber);

        Phone phoneObj = new Phone(phoneNumber);
        return verificationCodeService.sendSmsVerificationCode(phoneObj);
    }

    /**
     * Verifies a code for the given recipient.
     */
    public boolean verifyCode(String recipient, String code) {
        log.info("[应用服务] 验证验证码: recipient={}", recipient);
        return verificationCodeService.verifyCode(recipient, code);
    }
}
