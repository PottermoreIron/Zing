package com.pot.user.service.utils;

import com.pot.common.utils.JwtUtils;
import com.pot.user.service.controller.response.Tokens;

/**
 * @author: Pot
 * @created: 2025/8/16 22:34
 * @description: 用户通用工具类
 */
public class CommonUtils {
    public static Tokens createAccessTokenAndRefreshToken(Object claim) {
        // 创建访问令牌和刷新令牌
        String accessToken = JwtUtils.createAccessToken(claim);
        String refreshToken = JwtUtils.createRefreshToken(claim);
        // 返回令牌对象
        return Tokens.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
