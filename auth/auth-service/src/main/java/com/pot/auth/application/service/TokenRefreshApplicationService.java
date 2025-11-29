package com.pot.auth.application.service;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.valueobject.JwtToken;
import com.pot.auth.domain.authentication.valueobject.TokenPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Token刷新应用服务
 *
 * <p>负责Token的刷新和验证
 *
 * @author pot
 * @since 2025-11-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRefreshApplicationService {

    private final JwtTokenService jwtTokenService;

    /**
     * 刷新Token
     *
     * @param refreshTokenString RefreshToken字符串
     * @return 新的Token对
     */
    public LoginResponse refreshToken(String refreshTokenString) {
        log.info("[应用服务] 刷新Token");

        // 1. 调用领域服务刷新Token
        TokenPair tokenPair = jwtTokenService.refreshToken(refreshTokenString);

        // 2. 转换为应用层DTO
        JwtToken accessToken = tokenPair.accessToken();
        LoginResponse response = new LoginResponse(
                accessToken.userId().value(),
                accessToken.userDomain().name(),
                accessToken.username(),
                null, // email不在AccessToken中
                null, // phoneNumber不在AccessToken中
                tokenPair.getAccessTokenString(),
                tokenPair.getRefreshTokenString(),
                accessToken.expiresAt(),
                tokenPair.refreshToken().expiresAt()
        );

        log.info("[应用服务] Token刷新成功: userId={}", accessToken.userId());
        return response;
    }

    /**
     * 验证AccessToken
     *
     * @param accessTokenString AccessToken字符串
     * @return JWT Token信息
     */
    public JwtToken validateAccessToken(String accessTokenString) {
        log.debug("[应用服务] 验证AccessToken");
        return jwtTokenService.validateAccessToken(accessTokenString);
    }

    /**
     * 登出（将Token加入黑名单）
     *
     * @param accessTokenString AccessToken字符串
     */
    public void logout(String accessTokenString) {
        log.info("[应用服务] 用户登出");

        // 1. 验证Token
        JwtToken token = jwtTokenService.validateAccessToken(accessTokenString);

        // 2. 加入黑名单
        jwtTokenService.addToBlacklist(token.tokenId(), token.getRemainingSeconds());

        log.info("[应用服务] 用户登出成功: userId={}", token.userId());
    }
}

