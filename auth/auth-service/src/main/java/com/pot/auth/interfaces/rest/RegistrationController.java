package com.pot.auth.interfaces.rest;

import com.pot.auth.application.dto.RegisterResponse;
import com.pot.auth.application.service.RegistrationApplicationService;
import com.pot.auth.interfaces.assembler.AuthCommandAssembler;
import com.pot.auth.interfaces.dto.register.RegisterRequest;
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
 * Handles registration endpoints for all supported auth flows.
 *
 * @author pot
 * @since 2025-11-29
 */
@Tag(name = "Registration", description = "User registration supporting username-password, email, phone, OAuth2, WeChat, and more")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class RegistrationController {

    private final RegistrationApplicationService registrationApplicationService;
    private final AuthCommandAssembler authCommandAssembler;

    @Operation(operationId = "authRegister", summary = "Register", description = "Supports USERNAME_PASSWORD / EMAIL_PASSWORD / PHONE_PASSWORD / EMAIL_CODE / PHONE_CODE / OAUTH2 / WECHAT registration methods")
    @RateLimit(type = RateLimitMethodEnum.IP_BASED, rate = 3.0, message = "Too many registration requests, please try again later")
    @PostMapping("/api/v1/register")
    public R<RegisterResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("Registration request — registerType={}", request.registerType());

        RegisterResponse response = registrationApplicationService.register(authCommandAssembler.toCommand(request),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));

        return R.success(response);
    }
}