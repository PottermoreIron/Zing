package com.pot.user.service.controller;

import com.pot.common.R;
import com.pot.user.service.annotations.ratelimit.RateLimit;
import com.pot.user.service.controller.request.SendCodeRequest;
import com.pot.user.service.controller.request.register.RegisterRequest;
import com.pot.user.service.controller.response.Tokens;
import com.pot.user.service.enums.ratelimit.RateLimitType;
import com.pot.user.service.strategy.RegisterStrategy;
import com.pot.user.service.strategy.SendCodeStrategy;
import com.pot.user.service.strategy.factory.RegisterStrategyFactory;
import com.pot.user.service.strategy.factory.VerificationCodeStrategyFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author pot
 * @since 2025-02-25
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final RegisterStrategyFactory registerStrategyFactory;
    private final VerificationCodeStrategyFactory verificationCodeStrategyFactory;

    @RequestMapping("/register")
    public <T extends RegisterRequest> R<Tokens> register(@Valid @RequestBody T request) {
        log.info("request={}", request);
        RegisterStrategy<T> strategy = registerStrategyFactory.getStrategyByCode(request.getType());
        Tokens tokens = strategy.register(request);
        return R.success(tokens, "注册成功");
    }

    @RequestMapping("/send/code")
    public R<Void> sendSms(@Valid @RequestBody SendCodeRequest request) {
        SendCodeStrategy strategy = verificationCodeStrategyFactory.getStrategyByCode(request.getType());
        strategy.sendCode(request);
        return R.success("发送成功");
    }

    @RequestMapping("/test")
    @RateLimit(type = RateLimitType.IP_BASED, count = 1)
    public R<Void> test() {
        log.info("test");
        return R.success();
    }

    @RequestMapping("/test/token")
    public R<Void> testToken() {
        return R.success("token");
    }

}
