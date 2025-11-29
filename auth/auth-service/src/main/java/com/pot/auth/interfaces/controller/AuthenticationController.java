package com.pot.auth.interfaces.controller;

import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.service.OneStopAuthenticationService;
import com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest;
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
 * 一键认证控制器
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthenticationController {

    private final OneStopAuthenticationService authenticationService;

    @PostMapping("/api/v1/authenticate")
    public R<OneStopAuthResponse> authenticate(@Valid @RequestBody OneStopAuthRequest request, HttpServletRequest httpRequest) {

        log.info("[一键认证] 收到认证请求: authType={}, userDomain={}",
                request.authType(), request.userDomain());

        OneStopAuthResponse response = authenticationService.authenticate(request, getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));

        log.info("[一键认证] 认证成功: userId={}", response.userId());

        return R.success(response);
    }
}
