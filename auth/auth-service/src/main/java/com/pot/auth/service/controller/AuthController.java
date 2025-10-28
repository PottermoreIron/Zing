package com.pot.auth.service.controller;

import com.pot.auth.service.dto.request.LoginRequest;
import com.pot.auth.service.dto.request.RefreshTokenRequest;
import com.pot.auth.service.dto.request.RegisterRequest;
import com.pot.auth.service.dto.response.TokenResponse;
import com.pot.auth.service.service.AuthenticationService;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.security.annotation.PreventResubmit;
import com.pot.zing.framework.security.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * <p>
 * 处理用户认证相关的HTTP请求
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final SecurityProperties securityProperties;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @PreventResubmit(interval = 3, message = "登录操作过于频繁")
    public R<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());

        // 验证密码和确认密码
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }

        TokenResponse response = authenticationService.login(request);
        return R.success(response, "登录成功");
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @PreventResubmit(interval = 5, message = "注册操作过于频繁")
    public R<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("用户注册请求: email={}, phone={}", request.getEmail(), request.getPhone());

        // 验证密码和确认密码
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }

        // 至少提供邮箱或手机号之一
        if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getPhone())) {
            throw new BusinessException("邮箱和手机号至少填写一项");
        }

        TokenResponse response = authenticationService.register(request);
        return R.success(response, "注册成功");
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public R<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("刷新Token请求");

        TokenResponse response = authenticationService.refreshToken(request.getRefreshToken());
        return R.success(response, "Token刷新成功");
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        log.info("用户登出请求");

        // 从请求头中获取Token
        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            authenticationService.logout(token);
        }

        return R.success(null, "登出成功");
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public R<Object> getCurrentUser() {
        // TODO: 实现获取当前用户信息的逻辑
        return R.success(null, "获取用户信息成功");
    }

    /**
     * 从请求中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(securityProperties.getJwt().getHeader());
        String prefix = securityProperties.getJwt().getPrefix();

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(prefix)) {
            return bearerToken.substring(prefix.length());
        }

        return null;
    }
}


