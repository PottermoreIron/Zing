package com.pot.auth.interfaces.rest;

import com.pot.auth.application.service.VerificationCodeApplicationService;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.interfaces.validation.annotations.ValidEmail;
import com.pot.auth.interfaces.validation.annotations.ValidPhone;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles verification-code delivery endpoints.
 *
 * @author pot
 * @since 2025-11-10
 */
@Tag(name = "Verification Code", description = "Send email/SMS verification codes for registration, login, and other scenarios")
@Slf4j
@RestController
@RequestMapping("/auth/code")
@RequiredArgsConstructor
@Validated
public class VerificationCodeController {

    private final VerificationCodeApplicationService verificationCodeApplicationService;

    @Operation(operationId = "authSendEmailCode", summary = "Send email verification code", description = "Send a 6-digit verification code to the specified email address; rate-limiting and throttle policy are configuration-driven")
    @RateLimit(type = RateLimitMethodEnum.IP_BASED, key = "'verification-code:email:' + #email", rate = 1.0, message = "Verification code sent too frequently, please try again later")
    @PostMapping("/email")
    public R<Void> sendEmailCode(
            @RequestParam("email") @NotBlank(message = "Email address must not be blank") @ValidEmail(message = "Invalid email format") String email) {
        log.info("[API] Sending email verification code — email={}", email);

        boolean sent = verificationCodeApplicationService.sendEmailCode(email);

        if (sent) {
            return R.success();
        }
        return R.fail(AuthResultCode.CODE_SEND_FAILED);
    }

    @Operation(operationId = "authSendSmsCode", summary = "Send SMS verification code", description = "Send a 6-digit verification code to the specified phone number; rate-limiting and throttle policy are configuration-driven")
    @RateLimit(type = RateLimitMethodEnum.IP_BASED, key = "'verification-code:sms:' + #phone", rate = 1.0, message = "Verification code sent too frequently, please try again later")
    @PostMapping("/sms")
    public R<Void> sendSmsCode(
            @RequestParam("phone") @NotBlank(message = "Phone number must not be blank") @ValidPhone(message = "Invalid phone number format") String phone) {
        log.info("[API] Sending SMS verification code — phone={}", phone);

        boolean sent = verificationCodeApplicationService.sendSmsCode(phone);

        if (sent) {
            return R.success();
        }
        return R.fail(AuthResultCode.CODE_SEND_FAILED);
    }
}