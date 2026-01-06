package com.pot.auth.interfaces.controller;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.application.service.LoginApplicationService;
import com.pot.auth.application.service.TokenRefreshApplicationService;
import com.pot.auth.interfaces.dto.RefreshTokenRequest;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
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
 * 登录控制器
 *
 * <p>
 * 负责传统登录流程（要求用户已注册）
 *
 * <p>
 * 支持的登录方式：
 * <ul>
 * <li>用户名 + 密码</li>
 * <li>邮箱 + 密码</li>
 * <li>邮箱 + 验证码</li>
 * <li>手机号 + 验证码</li>
 * </ul>
 *
 * <p>
 * 注意：
 * <ul>
 * <li>如需一键认证（自动注册），请使用 {@link OneStopAuthenticationController}</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class LoginController {

    private final LoginApplicationService loginApplicationService;
    private final TokenRefreshApplicationService tokenRefreshApplicationService;

    /**
     * 传统登录入口
     *
     * <p>
     * 支持4种登录方式，通过loginType字段自动识别：
     * <ul>
     * <li>USERNAME_PASSWORD - 用户名密码登录</li>
     * <li>EMAIL_PASSWORD - 邮箱密码登录</li>
     * <li>EMAIL_CODE - 邮箱验证码登录</li>
     * <li>PHONE_CODE - 手机号验证码登录</li>
     * </ul>
     * 请求示例（用户名密码）：
     *
     * <pre>
     * POST /auth/api/v1/login
     * {
     *   "loginType": "USERNAME_PASSWORD",
     *   "username": "john_doe",
     *   "password": "Password123!",
     *   "userDomain": "MEMBER"
     * }
     * </pre>
     *
     * <p>
     * 请求示例（邮箱验证码）：
     *
     * <pre>
     * POST /auth/api/v1/login
     * {
     *   "loginType": "EMAIL_CODE",
     *   "email": "user@example.com",
     *   "code": "123456",
     *   "userDomain": "MEMBER"
     * }
     * </pre>
     */
    @RateLimit(type = RateLimitMethodEnum.IP_BASED, rate = 5.0, message = "登录请求过于频繁，请稍后再试")
    @PostMapping("/api/v1/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("[登录] 传统登录请求: loginType={}, userDomain={}",
                request.loginType(), request.userDomain());

        // 执行登录
        LoginResponse response = loginApplicationService.login(request, getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));

        return R.success(response);
    }

    /**
     * 刷新Token
     *
     * <p>
     * 使用 refreshToken 获取新的 accessToken
     *
     * <p>
     * 请求示例：
     *
     * <pre>
     * POST /auth/api/v1/refresh
     * {
     *   "refreshToken": "xxx"
     * }
     * </pre>
     */
    @RateLimit(type = RateLimitMethodEnum.IP_BASED, rate = 10.0, message = "Token刷新请求过于频繁，请稍后再试")
    @PostMapping("/api/v1/refresh")
    public R<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("[登录] 刷新Token请求");

        LoginResponse response = tokenRefreshApplicationService.refreshToken(request.refreshToken());

        return R.success(response);
    }
}
