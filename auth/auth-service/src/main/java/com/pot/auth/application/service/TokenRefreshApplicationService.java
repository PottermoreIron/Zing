package com.pot.auth.application.service;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.valueobject.JwtToken;
import com.pot.auth.domain.authentication.valueobject.TokenPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service for token refresh and validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRefreshApplicationService {

    private final JwtTokenService jwtTokenService;

    /**
     * Refreshes an access token with a refresh token.
     */
    public LoginResponse refreshToken(String refreshTokenString) {
        log.info("[AppService] Refreshing token");

        TokenPair tokenPair = jwtTokenService.refreshToken(refreshTokenString);

        JwtToken accessToken = tokenPair.accessToken();
        LoginResponse response = new LoginResponse(
                accessToken.userId().value(),
                accessToken.userDomain().name(),
                accessToken.nickname(),
                null,
                null,
                tokenPair.getAccessTokenString(),
                tokenPair.getRefreshTokenString(),
                accessToken.expiresAt(),
                tokenPair.refreshToken().expiresAt());

        log.info("[AppService] Token refreshed — userId={}", accessToken.userId());
        return response;
    }

    /**
     * Validates an access token.
     */
    public JwtToken validateAccessToken(String accessTokenString) {
        log.debug("[AppService] Validating AccessToken");
        return jwtTokenService.validateAccessToken(accessTokenString);
    }

    /**
     * Revokes the current access token by blacklisting it.
     */
    public void logout(String accessTokenString) {
        log.info("[AppService] User logout");

        JwtToken token = jwtTokenService.validateAccessToken(accessTokenString);
        jwtTokenService.addToBlacklist(token.tokenId(), token.getRemainingSeconds());

        log.info("[AppService] Logout successful — userId={}", token.userId());
    }
}
