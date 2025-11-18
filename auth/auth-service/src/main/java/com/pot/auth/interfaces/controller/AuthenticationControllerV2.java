package com.pot.auth.interfaces.controller;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.application.service.LoginApplicationService;
import com.pot.auth.application.service.TokenRefreshApplicationService;
import com.pot.auth.interfaces.dto.RefreshTokenRequest;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import com.pot.zing.framework.common.model.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（重构版）
 *
 * <p>提供认证相关的REST API
 * <p>支持7种登录方式的统一入口
 *
 * @author yecao
 * @since 2025-11-19
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthenticationControllerV2 {

    private final LoginApplicationService loginApplicationService;
    private final TokenRefreshApplicationService tokenRefreshApplicationService;

    /**
     * 统一登录入口
     *
     * <p>支持7种登录方式，通过loginType字段自动识别：
     * <ul>
     *   <li>USERNAME_PASSWORD - 用户名密码登录</li>
     *   <li>EMAIL_PASSWORD - 邮箱密码登录</li>
     *   <li>PHONE_PASSWORD - 手机号密码登录</li>
     *   <li>EMAIL_CODE - 邮箱验证码登录</li>
     *   <li>PHONE_CODE - 手机号验证码登录</li>
     *   <li>OAUTH2 - OAuth2登录（Google, GitHub等）</li>
     *   <li>WECHAT - 微信登录</li>
     * </ul>
     *
     * <p>请求示例（用户名密码）：
     * <pre>
     * POST /auth/v2/login
     * {
     *   "loginType": "USERNAME_PASSWORD",
     *   "username": "john_doe",
     *   "password": "Password123!",
     *   "userDomain": "MEMBER"
     * }
     * </pre>
     *
     * <p>请求示例（OAuth2）：
     * <pre>
     * POST /auth/v2/login
     * {
     *   "loginType": "OAUTH2",
     *   "provider": "google",
     *   "code": "4/0AY0e-g7...",
     *   "state": "random_state",
     *   "userDomain": "MEMBER"
     * }
     * </pre>
     */
    @PostMapping("/v2/login")
    public R<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("[接口V2] 登录请求: loginType={}", request.loginType());

        // 执行登录
        LoginResponse response = loginApplicationService.login(
                request,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        return R.success(response);
    }

    /**
     * 刷新Token
     *
     * <p>请求示例：
     * <pre>
     * POST /auth/v2/refresh
     * {
     *   "refreshToken": "xxx"
     * }
     * </pre>
     */
    @PostMapping("/v2/refresh")
    public R<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("[接口V2] 刷新Token请求");

        LoginResponse response = tokenRefreshApplicationService.refreshToken(request.refreshToken());

        return R.success(response);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多级代理，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "0.0.0.0";
    }
}

