package com.pot.member.service.controller;

import com.pot.common.R;
import com.pot.common.annotations.ratelimit.RateLimit;
import com.pot.common.enums.ResultCode;
import com.pot.common.enums.ratelimit.RateLimitMethodEnum;
import com.pot.member.service.controller.request.SendCodeRequest;
import com.pot.member.service.controller.request.register.RegisterRequest;
import com.pot.member.service.controller.response.Tokens;
import com.pot.member.service.strategy.OAuth2LoginStrategy;
import com.pot.member.service.strategy.RegisterStrategy;
import com.pot.member.service.strategy.SendCodeStrategy;
import com.pot.member.service.strategy.factory.OAuth2LoginStrategyFactory;
import com.pot.member.service.strategy.factory.RegisterStrategyFactory;
import com.pot.member.service.strategy.factory.VerificationCodeStrategyFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
@Tag(
        name = "用户相关接口",
        description = "提供用户注册、登录、验证码发送等功能"
)
@Slf4j
public class UserController {

    private final RegisterStrategyFactory registerStrategyFactory;
    private final VerificationCodeStrategyFactory verificationCodeStrategyFactory;
    private final OAuth2LoginStrategyFactory oAuth2LoginStrategyFactory;

    @RequestMapping("/register")
    public <T extends RegisterRequest> R<Tokens> register(@Valid @RequestBody T request) {
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
    @RateLimit(type = RateLimitMethodEnum.IP_BASED, count = 1)
    public R<Void> test() {
        log.info("test");
        return R.success();
    }

    @RequestMapping("/test/token")
    public R<Void> testToken() {
        return R.success("token");
    }

    @GetMapping("/oauth2/{provider}")
    public void redirectToOAuth2Login(@PathVariable("provider") String provider, HttpServletResponse response) {
        OAuth2LoginStrategy strategy = oAuth2LoginStrategyFactory.getStrategy(provider);
        strategy.redirectToOauth2Login(response);
    }

    @RequestMapping("/oauth2/{provider}/callback")
    public R<Tokens> handleOauth2Callback(@RequestParam("code") String code, @PathVariable("provider") String provider) {
        try {
            OAuth2LoginStrategy strategy = oAuth2LoginStrategyFactory.getStrategy(provider);
            Map<String, Object> userInfo = strategy.getOauth2UserInfo(code);
            Tokens tokens = strategy.loginOauth2User(userInfo);
            return R.success(tokens, "登录成功");
        } catch (Exception e) {
            log.error("OAuth2 login failed", e);
            return R.fail(ResultCode.OAUTH2_EXCEPTION, "OAuth2 login failed");
        }
    }
}
