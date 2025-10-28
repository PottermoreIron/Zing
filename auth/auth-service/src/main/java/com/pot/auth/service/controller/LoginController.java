package com.pot.auth.service.controller;

import com.pot.auth.service.dto.request.login.LoginRequest;
import com.pot.auth.service.dto.response.AuthResponse;
import com.pot.auth.service.service.LoginService;
import com.pot.zing.framework.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author: Pot
 * @created: 2025/10/20
 * @description: 登录控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "认证管理", description = "用户登录、退出、刷新Token等认证相关接口")
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持多种登录方式：用户名密码、手机号密码、邮箱密码、手机验证码、邮箱验证码")
    public R<AuthResponse> login(
            @Valid @RequestBody
            @Parameter(description = "登录请求，根据type字段自动识别登录方式")
            LoginRequest request) {
        log.info("收到登录请求: type={}", request.getType());
        AuthResponse response = loginService.login(request);
        return R.success(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "用户退出登录，清除相关会话信息")
    public R<Void> logout(
            @RequestParam
            @Parameter(description = "用户ID")
            Long userId) {
        log.info("收到退出登录请求: userId={}", userId);
        loginService.logout(userId);
        return R.success();
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用刷新令牌获取新的访问令牌")
    public R<AuthResponse> refreshToken(
            @RequestParam
            @Parameter(description = "刷新令牌")
            String refreshToken) {
        log.info("收到刷新Token请求");
        AuthResponse response = loginService.refreshToken(refreshToken);
        return R.success(response);
    }
}
