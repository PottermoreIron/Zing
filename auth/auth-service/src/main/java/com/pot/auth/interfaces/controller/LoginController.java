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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.pot.zing.framework.common.util.IpUtils.getClientIp;

/**
 * 登录控制器
 *
 * <p>
 * 负责传统登录流程，要求用户已注册
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
     * 统一登录入口
     *
     * <p>
     * 支持6种登录方式（已移除手机号密码登录），通过loginType字段自动识别：
     * <ul>
     * <li>USERNAME_PASSWORD - 用户名密码登录</li>
     * <li>EMAIL_PASSWORD - 邮箱密码登录</li>
     * <li>EMAIL_CODE - 邮箱验证码登录</li>
     * <li>PHONE_CODE - 手机号验证码登录</li>
     * <li>OAUTH2 - OAuth2登录（Google, GitHub等）</li>
     * <li>WECHAT - 微信登录</li>
     * </ul>
     *
     * <p>
     * 请求示例（用户名密码）：
     *
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
     * <p>
     * 请求示例（OAuth2）：
     *
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
    @PostMapping("api/v1/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("登录请求: loginType={}", request.loginType());

        // 执行登录
        LoginResponse response = loginApplicationService.login(request, getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));

        return R.success(response);
    }

    /**
     * 刷新Token
     *
     * <p>
     * 请求示例：
     *
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
}
