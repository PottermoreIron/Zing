package com.pot.auth.interfaces.rest;

import com.pot.auth.application.service.LogoutApplicationService;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.interfaces.dto.LogoutRequest;
import com.pot.zing.framework.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Revokes the current access token and an optional refresh token.
 *
 * @author pot
 * @since 2025-12-14
 */
@Tag(name = "Logout", description = "Revoke the AccessToken and RefreshToken to prevent continued use after sign-out")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LogoutController {

    private final LogoutApplicationService logoutApplicationService;

    @Operation(operationId = "authLogout", summary = "Logout", description = "Blacklist the current AccessToken and optionally remove the RefreshToken")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/logout")
    public R<Void> logout(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) LogoutRequest request) {

        String authorization = httpRequest.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            log.warn("[Logout] Missing Authorization header");
            return R.fail(AuthResultCode.TOKEN_INVALID);
        }

        String accessToken = authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim()
                : authorization.trim();

        String refreshToken = (request != null) ? request.refreshToken() : null;

        logoutApplicationService.logout(accessToken, refreshToken);

        return R.success(null);
    }
}