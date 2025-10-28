package com.pot.auth.service.utils;

import com.pot.auth.service.dto.response.AuthToken;
import com.pot.zing.framework.common.properties.JwtProperties;
import com.pot.zing.framework.common.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/10/15 23:12
 * @description: 用户Token工具类
 */
@Component
@RequiredArgsConstructor
public class UserTokenUtils {
    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;

    public AuthToken createAccessTokenAndRefreshToken(Object claim) {
        // 创建访问令牌和刷新令牌
        String accessToken = jwtUtils.createAccessToken(claim);
        String refreshToken = jwtUtils.createRefreshToken(claim);
        // 获取过期时间, 单位秒
        Long accessExpiresIn = jwtProperties.getAccessTokenExpiration() / 1000;
        Long refreshExpiresIn = jwtProperties.getRefreshTokenExpiration() / 1000;
        // 返回令牌对象
        return AuthToken.builder()
                .accessToken(accessToken)
                .accessExpiresIn(accessExpiresIn)
                .refreshToken(refreshToken)
                .refreshExpiresIn(refreshExpiresIn)
                .tokenType("Bearer")
                .build();
    }
}
