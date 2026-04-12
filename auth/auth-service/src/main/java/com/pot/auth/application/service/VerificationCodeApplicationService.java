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
        log.info("[AppService] Sending email verification code — email={}", email);

        Email emailObj = new Email(email);
        return verificationCodeService.sendEmailVerificationCode(emailObj);
    }

    /**
     * Sends an SMS verification code.
     */
    public boolean sendSmsCode(String phoneNumber) {
        log.info("[AppService] Sending SMS verification code — phone={}", phoneNumber);

        Phone phoneObj = new Phone(phoneNumber);
        return verificationCodeService.sendSmsVerificationCode(phoneObj);
    }

    /**
     * Verifies a code for the given recipient.
     */
    public boolean verifyCode(String recipient, String code) {
        log.info("[AppService] Verifying code — recipient={}", recipient);
        return verificationCodeService.verifyCode(recipient, code);
    }
}
