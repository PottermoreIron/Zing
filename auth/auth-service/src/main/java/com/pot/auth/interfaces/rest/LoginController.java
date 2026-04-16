package com.pot.auth.interfaces.rest;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.application.service.LoginApplicationService;
import com.pot.auth.application.service.TokenRefreshApplicationService;
import com.pot.auth.interfaces.assembler.AuthCommandAssembler;
import com.pot.auth.interfaces.dto.RefreshTokenRequest;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
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
 * Handles traditional login and token refresh endpoints.
 *
 * @author pot
 * @since 2025-11-29
 */
@Tag(name = "Login", description = "Traditional login (all login types require a verification code except USERNAME_PASSWORD) and token refresh")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class LoginController {

    private final LoginApplicationService loginApplicationService;
    private final TokenRefreshApplicationService tokenRefreshApplicationService;
    private final AuthCommandAssembler authCommandAssembler;

    @Operation(operationId = "authLogin", summary = "Login", description = "Supports USERNAME_PASSWORD / EMAIL_PASSWORD / PHONE_PASSWORD / EMAIL_CODE / PHONE_CODE login methods. EMAIL_PASSWORD and PHONE_PASSWORD require a verification code in addition to the password")
    @RateLimit(type = RateLimitMethodEnum.IP_BASED, rate = 5.0, message = "Too many login requests, please try again later")
    @PostMapping("/api/v1/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("[Login] Login request — loginType={}, userDomain={}",
                request.loginType(), request.userDomain());

        LoginResponse response = loginApplicationService.login(authCommandAssembler.toCommand(request),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));

        return R.success(response);
    }

    @Operation(operationId = "authRefreshToken", summary = "Refresh token", description = "Exchange a refreshToken for a new accessToken; the refreshToken auto-renews within its sliding window")
    @RateLimit(type = RateLimitMethodEnum.IP_BASED, rate = 10.0, message = "Too many token refresh requests, please try again later")
    @PostMapping("/api/v1/refresh")
    public R<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("[Login] Token refresh request");

        LoginResponse response = tokenRefreshApplicationService.refreshToken(request.refreshToken());

        return R.success(response);
    }
}