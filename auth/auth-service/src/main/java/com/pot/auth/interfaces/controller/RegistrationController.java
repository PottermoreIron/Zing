package com.pot.auth.interfaces.controller;

import com.pot.auth.application.dto.RegisterResponse;
import com.pot.auth.application.service.RegistrationApplicationService;
import com.pot.auth.interfaces.dto.register.RegisterRequest;
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
 * 注册控制器
 * 提供用户注册相关的REST API
 * 支持6种注册方式的统一入口
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class RegistrationController {

    private final RegistrationApplicationService registrationApplicationService;

    /**
     * 统一注册入口
     *
     * <p>
     * 支持6种注册方式，通过registerType字段自动识别：
     * <ul>
     * <li>USERNAME_PASSWORD - 用户名密码注册</li>
     * <li>EMAIL_PASSWORD - 邮箱密码注册</li>
     * <li>EMAIL_CODE - 邮箱验证码注册</li>
     * <li>PHONE_CODE - 手机号验证码注册</li>
     * <li>OAUTH2 - OAuth2注册（Google, GitHub等）</li>
     * <li>WECHAT - 微信注册</li>
     * </ul>
     */
    @PostMapping("api/v1/register")
    public R<RegisterResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("注册请求: registerType={}", request.registerType());

        // 执行注册
        RegisterResponse response = registrationApplicationService.register(request, getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));

        return R.success(response);
    }
}
