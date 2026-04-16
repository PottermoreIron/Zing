package com.pot.auth.interfaces.rest;

import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.service.OneStopAuthenticationService;
import com.pot.auth.interfaces.assembler.AuthCommandAssembler;
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
@Tag(name = "One-Stop Authentication", description = "Frictionless authentication: auto-registers new users and directly logs in existing ones, supporting all authentication methods")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class OneStopAuthenticationController {

        private final OneStopAuthenticationService oneStopAuthenticationService;
        private final AuthCommandAssembler authCommandAssembler;

        @Operation(operationId = "authAuthenticateOneStop", summary = "Authenticate (auto-register or login)", description = "Supports EMAIL_PASSWORD / PHONE_PASSWORD / EMAIL_CODE / PHONE_CODE / USERNAME_PASSWORD / OAUTH2 / WECHAT. Auto-registers if user not found")
        @RateLimit(type = RateLimitMethodEnum.IP_BASED, rate = 5.0, message = "Too many authentication requests, please try again later")
        @PostMapping("/api/v1/authenticate")
        public R<OneStopAuthResponse> authenticate(
                        @Valid @RequestBody OneStopAuthRequest request,
                        HttpServletRequest httpRequest) {

                log.info("[OneStopAuth] Authentication request received — authType={}, userDomain={}",
                                request.authType(), request.userDomain());

                OneStopAuthResponse response = oneStopAuthenticationService.authenticate(
                                authCommandAssembler.toCommand(request),
                                getClientIp(httpRequest),
                                httpRequest.getHeader("User-Agent"));

                log.info("[OneStopAuth] Authentication successful — userId={}", response.userId());

                return R.success(response);
        }
}