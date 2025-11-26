package com.pot.auth.interfaces.controller;

import com.pot.auth.application.dto.RegisterResponse;
import com.pot.auth.application.service.RegistrationApplicationService;
import com.pot.auth.interfaces.dto.auth.RegisterRequest;
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

/**
 * 注册控制器（重构版）
 *
 * <p>提供用户注册相关的REST API
 * <p>支持7种注册方式的统一入口
 *
 * @author yecao
 * @since 2025-11-19
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class RegistrationControllerV2 {

    private final RegistrationApplicationService registrationApplicationService;

    /**
     * 统一注册入口
     *
     * <p>支持7种注册方式，通过registerType字段自动识别：
     * <ul>
     *   <li>USERNAME_PASSWORD - 用户名密码注册</li>
     *   <li>EMAIL_PASSWORD - 邮箱密码注册</li>
     *   <li>PHONE_PASSWORD - 手机号密码注册</li>
     *   <li>EMAIL_CODE - 邮箱验证码注册</li>
     *   <li>PHONE_CODE - 手机号验证码注册</li>
     *   <li>OAUTH2 - OAuth2注册（Google, GitHub等）</li>
     *   <li>WECHAT - 微信注册</li>
     * </ul>
     *
     * <p>请求示例（用户名密码）：
     * <pre>
     * POST /auth/v2/register
     * {
     *   "registerType": "USERNAME_PASSWORD",
     *   "username": "john_doe",
     *   "password": "Password123!",
     *   "userDomain": "MEMBER"
     * }
     * </pre>
     *
     * <p>请求示例（邮箱验证码）：
     * <pre>
     * POST /auth/v2/register
     * {
     *   "registerType": "EMAIL_CODE",
     *   "email": "john@example.com",
     *   "verificationCode": "123456",
     *   "userDomain": "MEMBER"
     * }
     * </pre>
     *
     * <p>请求示例（OAuth2）：
     * <pre>
     * POST /auth/v2/register
     * {
     *   "registerType": "OAUTH2",
     *   "provider": "google",
     *   "code": "4/0AY0e-g7...",
     *   "state": "random_state",
     *   "userDomain": "MEMBER"
     * }
     * </pre>
     */
    @PostMapping("/v2/register")
    public R<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("[接口V2] 注册请求: registerType={}", request.registerType());

        // 执行注册
        RegisterResponse response = registrationApplicationService.register(
                request,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

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

