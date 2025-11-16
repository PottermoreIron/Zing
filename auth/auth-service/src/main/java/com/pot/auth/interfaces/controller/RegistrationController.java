package com.pot.auth.interfaces.controller;

import com.pot.auth.application.command.RegisterCommand;
import com.pot.auth.application.dto.RegisterResponse;
import com.pot.auth.application.service.RegistrationApplicationService;
import com.pot.auth.interfaces.dto.register.EmailRegisterRequest;
import com.pot.auth.interfaces.dto.register.PhoneRegisterRequest;
import com.pot.auth.interfaces.dto.register.UsernameRegisterRequest;
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
 * 注册控制器
 *
 * <p>提供用户注册相关的REST API
 *
 * @author yecao
 * @since 2025-11-10
 */
@Slf4j
@RestController
@RequestMapping("/auth/register")
@RequiredArgsConstructor
@Validated
public class RegistrationController {

    private final RegistrationApplicationService registrationApplicationService;

    /**
     * 用户名密码注册
     * <p>
     * POST /auth/register/username
     * {
     * "username": "john_doe",
     * "password": "Password123",
     * "userDomain": "MEMBER"
     * }
     *
     * <p>这是最简单的注册方式，不需要验证码
     */
    @PostMapping("/username")
    public R<RegisterResponse> registerWithUsername(
            @RequestBody UsernameRegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("[接口] 用户名注册请求: username={}", request.username());

        // 构建注册命令
        RegisterCommand command = RegisterCommand.forUsername(
                request.getUserDomainEnum(),
                request.username(),
                request.password(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        // 执行注册
        RegisterResponse response = registrationApplicationService.registerWithUsername(command);

        return R.success(response);
    }

    /**
     * 邮箱密码注册
     * <p>
     * POST /auth/register/email
     * {
     * "email": "john@example.com",
     * "password": "Password123",
     * "verificationCode": "123456",
     * "userDomain": "MEMBER"
     * }
     *
     * <p>使用邮箱作为主要标识，系统会自动生成用户名
     */
    @PostMapping("/email")
    public R<RegisterResponse> registerWithEmail(
            @Valid @RequestBody EmailRegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("[接口] 邮箱注册请求: email={}", request.email());

        // 构建注册命令
        RegisterCommand command = RegisterCommand.forEmail(
                request.getUserDomainEnum(),
                request.email(),
                request.password(),
                request.verificationCode(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        // 执行注册
        RegisterResponse response = registrationApplicationService.registerWithEmail(command);

        return R.success(response);
    }

    /**
     * 手机号密码注册
     * <p>
     * POST /auth/register/phone
     * {
     * "phone": "13800138000",
     * "password": "Password123",
     * "verificationCode": "123456",
     * "userDomain": "MEMBER"
     * }
     *
     * <p>使用手机号作为主要标识，系统会自动生成用户名
     */
    @PostMapping("/phone")
    public R<RegisterResponse> registerWithPhone(
            @Valid @RequestBody PhoneRegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("[接口] 手机号注册请求: phone={}", request.phone());

        // 构建注册命令
        RegisterCommand command = RegisterCommand.forPhone(
                request.getUserDomainEnum(),
                request.phone(),
                request.password(),
                request.verificationCode(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        // 执行注册
        RegisterResponse response = registrationApplicationService.registerWithPhone(command);

        return R.success(response);
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "0.0.0.0";
    }
}
