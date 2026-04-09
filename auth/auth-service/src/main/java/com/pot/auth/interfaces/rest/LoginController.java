package com.pot.auth.interfaces.rest;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.application.service.LoginApplicationService;
import com.pot.auth.application.service.TokenRefreshApplicationService;
import com.pot.auth.interfaces.assembler.AuthCommandAssembler;
import com.pot.auth.interfaces.dto.RefreshTokenRequest;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.pot.zing.framework.common.util.IpUtils.getClientIp;

/**
 * Handles traditional login and token refresh endpoints.
 *
 * @author pot
 * @since 2025-11-29
 */
@Tag(name = "登录", description = "传统登录（昵称密码 / 邮箱密码 / 验证码）和 Token 刷新")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class LoginController {

    private final LoginApplicationService loginApplicationService;
    private final TokenRefreshApplicationService tokenRefreshApplicationService;
    private final AuthCommandAssembler authCommandAssembler;

    @Operation(operationId = "authLogin", summary = "传统登录", description = "支持 USERNAME_PASSWORD / EMAIL_PASSWORD / EMAIL_CODE / PHONE_CODE 四种登录方式")
    @RateLimit(type = RateLimitMethodEnum.IP_BASED, rate = 5.0, message = "登录请求过于频繁，请稍后再试")
    @PostMapping("/api/v1/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("[登录] 传统登录请求: loginType={}, userDomain={}",
                request.loginType(), request.userDomain());

        LoginResponse response = loginApplicationService.login(authCommandAssembler.toCommand(request),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));

        return R.success(response);
    }

    @Operation(operationId = "authRefreshToken", summary = "刷新 Token", description = "使用 refreshToken 换取新的 accessToken，refreshToken 在滑动窗口内自动续期")
    @RateLimit(type = RateLimitMethodEnum.IP_BASED, rate = 10.0, message = "Token刷新请求过于频繁，请稍后再试")
    @PostMapping("/api/v1/refresh")
    public R<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("[登录] 刷新Token请求");

        LoginResponse response = tokenRefreshApplicationService.refreshToken(request.refreshToken());

        return R.success(response);
    }
}