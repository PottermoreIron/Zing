package com.pot.auth.interfaces.controller;

import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.service.OneStopAuthenticationService;
import com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest;
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
 * Handles unified authenticate-or-register flows.
 *
 * @author pot
 * @since 2025-11-30
 */
@Tag(name = "一键认证", description = "无感认证：用户不存在自动注册，已存在直接登录，支持所有认证方式")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class OneStopAuthenticationController {

        private final OneStopAuthenticationService oneStopAuthenticationService;

        @Operation(summary = "一键认证（自动注册/登录）", description = "支持 USERNAME_PASSWORD / PHONE_CODE / EMAIL_CODE / OAUTH2 / WECHAT 等方式，用户不存在时自动注册")
        @RateLimit(type = RateLimitMethodEnum.IP_BASED, rate = 5.0, message = "认证请求过于频繁，请稍后再试")
        @PostMapping("/api/v1/authenticate")
        public R<OneStopAuthResponse> authenticate(
                        @Valid @RequestBody OneStopAuthRequest request,
                        HttpServletRequest httpRequest) {

                log.info("[一键认证] 收到认证请求: authType={}, userDomain={}",
                                request.authType(), request.userDomain());

                OneStopAuthResponse response = oneStopAuthenticationService.authenticate(
                                request,
                                getClientIp(httpRequest),
                                httpRequest.getHeader("User-Agent"));

                log.info("[一键认证] 认证成功: userId={}", response.userId());

                return R.success(response);
        }
}
