package com.pot.auth.application.service;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 登出应用服务
 *
 * <p>
 * 职责：
 * <ul>
 * <li>将 AccessToken 加入 Redis 黑名单（使其提前失效）</li>
 * <li>删除 RefreshToken 缓存（阻止续期）</li>
 * </ul>
 *
 * <p>
 * 容错设计：Token 解析失败时不抛出异常，保证登出操作的幂等性。
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutApplicationService {

    private final JwtTokenService jwtTokenService;

    /**
     * 执行登出
     *
     * @param accessToken  Access Token 字符串（必填）
     * @param refreshToken Refresh Token 字符串（可选，提供则同步吊销）
     */
    public void logout(String accessToken, String refreshToken) {
        log.info("[登出] 执行登出操作");
        jwtTokenService.logout(accessToken, refreshToken);
        log.info("[登出] 登出操作完成");
    }
}
