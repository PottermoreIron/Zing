package com.pot.auth.interfaces.controller;

import com.pot.auth.application.service.VerificationCodeApplicationService;
import com.pot.auth.domain.validation.annotations.ValidEmail;
import com.pot.auth.domain.validation.annotations.ValidPhone;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码控制器
 *
 * <p>
 * 提供验证码相关的REST API
 *
 * @author pot
 * @since 2025-11-10
 */
@Slf4j
@RestController
@RequestMapping("/auth/code")
@RequiredArgsConstructor
@Validated
public class VerificationCodeController {

    private final VerificationCodeApplicationService verificationCodeApplicationService;

    /**
     * 发送邮件验证码
     * <p>
     * POST /auth/code/email?email=xxx@example.com
     */
    @RateLimit(type = RateLimitMethodEnum.IP_BASED, rate = 1.0, message = "验证码发送过于频繁，请稍后再试")
    @PostMapping("/email")
    public R<Void> sendEmailCode(
            @RequestParam @NotBlank(message = "邮箱不能为空") @ValidEmail(message = "邮箱格式不正确") String email) {
        log.info("[接口] 发送邮件验证码: email={}", email);

        boolean sent = verificationCodeApplicationService.sendEmailCode(email);

        if (sent) {
            return R.success();
        } else {
            return R.fail("验证码发送失败");
        }
    }

    /**
     * 发送短信验证码
     * <p>
     * POST /auth/code/sms?phone=+8613800138000
     */
    @RateLimit(type = RateLimitMethodEnum.IP_BASED, rate = 1.0, message = "验证码发送过于频繁，请稍后再试")
    @PostMapping("/sms")
    public R<Void> sendSmsCode(
            @RequestParam @NotBlank(message = "手机号不能为空") @ValidPhone(message = "手机号格式不正确") String phone) {
        log.info("[接口] 发送短信验证码: phone={}", phone);

        boolean sent = verificationCodeApplicationService.sendSmsCode(phone);

        if (sent) {
            return R.success();
        } else {
            return R.fail("验证码发送失败");
        }
    }
}
