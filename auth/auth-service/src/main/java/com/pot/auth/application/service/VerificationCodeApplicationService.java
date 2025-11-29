package com.pot.auth.application.service;

import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.Phone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 验证码应用服务
 *
 * <p>负责验证码的发送和验证
 *
 * @author pot
 * @since 2025-11-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeApplicationService {

    private final VerificationCodeService verificationCodeService;

    /**
     * 发送邮件验证码
     *
     * @param email 邮箱地址
     * @return 是否发送成功
     */
    public boolean sendEmailCode(String email) {
        log.info("[应用服务] 发送邮件验证码: email={}", email);

        Email emailObj = new Email(email);
        return verificationCodeService.sendEmailVerificationCode(emailObj);
    }

    /**
     * 发送短信验证码
     *
     * @param phoneNumber 手机号
     * @return 是否发送成功
     */
    public boolean sendSmsCode(String phoneNumber) {
        log.info("[应用服务] 发送短信验证码: phone={}", phoneNumber);

        Phone phoneObj = new Phone(phoneNumber);
        return verificationCodeService.sendSmsVerificationCode(phoneObj);
    }

    /**
     * 验证验证码
     *
     * @param recipient 接收者（邮箱或手机号）
     * @param code      验证码
     * @return 是否验证成功
     */
    public boolean verifyCode(String recipient, String code) {
        log.info("[应用服务] 验证验证码: recipient={}", recipient);
        return verificationCodeService.verifyCode(recipient, code);
    }
}

