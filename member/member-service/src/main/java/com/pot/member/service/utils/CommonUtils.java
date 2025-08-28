package com.pot.member.service.utils;

import com.pot.common.utils.JwtUtils;
import com.pot.member.service.controller.response.Tokens;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/8/16 22:34
 * @description: 用户通用工具类
 */
@Component
@RequiredArgsConstructor
public class CommonUtils {

    private final JwtUtils jwtUtils;

    public Tokens createAccessTokenAndRefreshToken(Object claim) {
        // 创建访问令牌和刷新令牌
        String accessToken = jwtUtils.createAccessToken(claim);
        String refreshToken = jwtUtils.createRefreshToken(claim);
        // 返回令牌对象
        return Tokens.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
