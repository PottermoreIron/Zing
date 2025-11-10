package com.pot.auth.interfaces.controller;

import com.pot.auth.application.command.LoginCommand;
import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.application.service.CodeLoginApplicationService;
import com.pot.auth.application.service.LoginApplicationService;
import com.pot.auth.application.service.TokenRefreshApplicationService;
import com.pot.auth.domain.authentication.valueobject.JwtToken;
import com.pot.auth.interfaces.dto.RefreshTokenRequest;
import com.pot.auth.interfaces.dto.login.EmailCodeLoginRequest;
import com.pot.auth.interfaces.dto.login.PasswordLoginRequest;
import com.pot.auth.interfaces.dto.login.PhoneCodeLoginRequest;
import com.pot.zing.framework.common.model.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 认证控制器
 *
 * <p>提供认证相关的REST API
 *
 * @author yecao
 * @since 2025-11-10
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthenticationController {

    private final LoginApplicationService loginApplicationService;
    private final CodeLoginApplicationService codeLoginApplicationService;
    private final TokenRefreshApplicationService tokenRefreshApplicationService;

    /**
     * 密码登录
     * <p>
     * POST /auth/login/password
     * {
     * "identifier": "john_doe",
     * "password": "Password123",
     * "userDomain": "MEMBER"
     * }
     */
    @PostMapping("/login/password")
    public R<LoginResponse> loginWithPassword(
            @RequestBody PasswordLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("[接口] 密码登录请求: identifier={}", request.identifier());

        // 构建登录命令
        LoginCommand command = LoginCommand.of(
                request.identifier(),
                request.password(),
                request.getUserDomainEnum(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        // 执行登录
        LoginResponse response = loginApplicationService.loginWithPassword(command);

        return R.success(response);
    }

    /**
     * 邮箱验证码登录
     * <p>
     * POST /auth/login/email-code
     * {
     * "email": "john@example.com",
     * "verificationCode": "123456",
     * "userDomain": "MEMBER"
     * }
     */
    @PostMapping("/login/email-code")
    public R<LoginResponse> loginWithEmailCode(
            @RequestBody EmailCodeLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("[接口] 邮箱验证码登录请求: email={}", request.email());

        // 执行验证码登录
        LoginResponse response = codeLoginApplicationService.loginWithEmailCode(
                request.email(),
                request.verificationCode(),
                request.getUserDomainEnum(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        return R.success(response);
    }

    /**
     * 手机验证码登录
     * <p>
     * POST /auth/login/phone-code
     * {
     * "phone": "13800138000",
     * "verificationCode": "123456",
     * "userDomain": "MEMBER"
     * }
     */
    @PostMapping("/login/phone-code")
    public R<LoginResponse> loginWithPhoneCode(
            @RequestBody PhoneCodeLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("[接口] 手机验证码登录请求: phone={}", request.phone());

        // 执行验证码登录
        LoginResponse response = codeLoginApplicationService.loginWithPhoneCode(
                request.phone(),
                request.verificationCode(),
                request.getUserDomainEnum(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        return R.success(response);
    }

    /**
     * 刷新Token
     * <p>
     * POST /auth/refresh
     * {
     * "refreshToken": "xxx"
     * }
     */
    @PostMapping("/refresh")
    public R<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("[接口] 刷新Token请求");

        LoginResponse response = tokenRefreshApplicationService.refreshToken(request.refreshToken());

        return R.success(response);
    }

    /**
     * 验证Token
     * <p>
     * GET /auth/validate?token=xxx
     */
    @GetMapping("/validate")
    public R<JwtToken> validateToken(@RequestParam @NotBlank(message = "token不能为空") String token) {
        log.debug("[接口] 验证Token请求");

        JwtToken jwtToken = tokenRefreshApplicationService.validateAccessToken(token);

        return R.success(jwtToken);
    }

    /**
     * 登出
     * <p>
     * POST /auth/logout
     * Header: Authorization: Bearer xxx
     */
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") @NotBlank(message = "authorization不能为空") String authorization) {
        log.info("[接口] 登出请求");

        // 提取Token
        String token = authorization.replace("Bearer ", "");

        tokenRefreshApplicationService.logout(token);

        return R.success();
    }

    /**
     * 获取客户端真实IP
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
        return ip != null ? ip : "unknown";
    }
}

