package com.pot.auth.domain.authentication.service;

import com.pot.auth.domain.authentication.valueobject.JwtToken;
import com.pot.auth.domain.authentication.valueobject.RefreshToken;
import com.pot.auth.domain.authentication.valueobject.TokenPair;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.port.TokenManagementPort;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.TokenId;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * JWT Token领域服务
 *
 * <p>负责Token的生命周期管理：
 * <ul>
 *   <li>生成Token对（AccessToken + RefreshToken）</li>
 *   <li>验证Token有效性</li>
 *   <li>刷新Token（滑动窗口续期）</li>
 *   <li>黑名单管理</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final TokenManagementPort tokenManagementPort;
    private final CachePort cachePort;
    private final UserModulePort userModulePort;

    @Value("${auth.token.jwt.refresh-token-ttl:2592000}")
    private long refreshTokenTtl; // 30天
    private long refreshTokenSlidingWindow; // 7天

    @Value("${auth.token.jwt.refresh-token-sliding-window:604800}")
    /**
     * 生成Token对（AccessToken + RefreshToken）
     *
     * @param userId      用户ID
     * @param userDomain  用户域
     * @param username    用户名
     * @param authorities 权限集合
     * @return Token对
     */
    public TokenPair generateTokenPair(
            UserId userId,
            UserDomain userDomain,
            String username,
            Set<String> authorities
    ) {
        log.info("[Token] 生成Token对: userId={}, userDomain={}, username={}", userId, userDomain, username);

        TokenPair tokenPair = tokenManagementPort.generateTokenPair(userId, userDomain, username, authorities);

        // 存储RefreshToken到缓存
        storeRefreshToken(tokenPair.refreshToken());

        return tokenPair;
    }

    /**
     * 验证AccessToken
     *
     * @param tokenString Token字符串
     * @return JwtToken
     */
    public JwtToken validateAccessToken(String tokenString) {
        log.debug("[Token] 验证AccessToken");

        // 1. 解析Token
        JwtToken token = tokenManagementPort.parseAccessToken(tokenString);

        // 2. 检查是否过期
        if (token.isExpired()) {
            log.warn("[Token] AccessToken已过期: tokenId={}", token.tokenId());
            throw new TokenExpiredException("AccessToken已过期");
        }

        // 3. 检查是否在黑名单
        if (isInBlacklist(token.tokenId())) {
            log.warn("[Token] AccessToken在黑名单中: tokenId={}", token.tokenId());
            throw new TokenInvalidException("Token已失效");
        }

        log.debug("[Token] AccessToken验证成功: tokenId={}", token.tokenId());
        return token;
    }

    /**
     * 刷新Token（滑动窗口续期）
     *
     * @param refreshTokenString RefreshToken字符串
     * @return 新的TokenPair
     */
    public TokenPair refreshToken(String refreshTokenString) {
        log.info("[Token] 开始刷新Token");

        // 1. 解析RefreshToken
        RefreshToken oldRefreshToken = tokenManagementPort.parseRefreshToken(refreshTokenString);

        // 2. 检查是否过期
        if (oldRefreshToken.isExpired()) {
            log.warn("[Token] RefreshToken已过期: tokenId={}", oldRefreshToken.tokenId());
            throw new TokenExpiredException("RefreshToken已过期，请重新登录");
        }

        // 3. 检查RefreshToken是否存在于缓存（验证是否被撤销）
        String cacheKey = "auth:refresh:" + oldRefreshToken.tokenId().value();
        if (!cachePort.exists(cacheKey)) {
            log.warn("[Token] RefreshToken不存在或已被撤销: tokenId={}", oldRefreshToken.tokenId());
            throw new TokenInvalidException("RefreshToken已失效，请重新登录");
        }

        // 4. 获取用户权限
        Set<String> authorities = userModulePort.getPermissions(oldRefreshToken.userId());

        // 5. 生成新的TokenPair
        TokenPair newTokenPair = tokenManagementPort.generateTokenPair(
                oldRefreshToken.userId(),
                oldRefreshToken.userDomain(),
                getUsernameFromCache(oldRefreshToken),
                authorities
        );

        // 6. 删除旧的RefreshToken缓存
        cachePort.delete(cacheKey);

        // 7. 如果在滑动窗口内，使用新的RefreshToken；否则复用旧的
        if (oldRefreshToken.isWithinSlidingWindow(refreshTokenSlidingWindow)) {
            log.info("[Token] RefreshToken在滑动窗口内，已续期: tokenId={}", oldRefreshToken.tokenId());
            // 已经生成了新的RefreshToken，存储到缓存
            storeRefreshToken(newTokenPair.refreshToken());
        } else {
            log.info("[Token] RefreshToken不在滑动窗口内，复用旧Token: tokenId={}", oldRefreshToken.tokenId());
            // 恢复旧的RefreshToken缓存
            storeRefreshToken(oldRefreshToken);
        }

        log.info("[Token] Token刷新成功: userId={}", oldRefreshToken.userId());
        return newTokenPair;
    }

    /**
     * 将Token加入黑名单（登出）
     *
     * @param tokenId          Token ID
     * @param remainingSeconds Token剩余有效时间
     */
    public void addToBlacklist(TokenId tokenId, long remainingSeconds) {
        log.info("[Token] 将Token加入黑名单: tokenId={}, ttl={}s", tokenId, remainingSeconds);

        String blacklistKey = "auth:blacklist:" + tokenId.value();
        cachePort.set(blacklistKey, "1", Duration.ofSeconds(remainingSeconds));
    }

    /**
     * 检查Token是否在黑名单
     */
    private boolean isInBlacklist(TokenId tokenId) {
        String blacklistKey = "auth:blacklist:" + tokenId.value();
        return cachePort.exists(blacklistKey);
    }

    /**
     * 存储RefreshToken到缓存
     */
    private void storeRefreshToken(RefreshToken refreshToken) {
        String cacheKey = "auth:refresh:" + refreshToken.tokenId().value();
        long ttl = refreshToken.getRemainingSeconds();
        cachePort.set(cacheKey, refreshToken.rawToken(), Duration.ofSeconds(ttl));
    }

    /**
     * 从缓存获取用户名（简化实现）
     */
    private String getUsernameFromCache(RefreshToken refreshToken) {
        // TODO: 从缓存或用户模块获取用户名
        // 临时方案：从userId获取用户信息
        return userModulePort.findById(refreshToken.userId())
                .map(UserDTO::username)
                .orElse("unknown");
    }

    /**
     * Token过期异常
     */
    public static class TokenExpiredException extends DomainException {
        public TokenExpiredException(String message) {
            super(message);
        }
    }

    /**
     * Token无效异常
     */
    public static class TokenInvalidException extends DomainException {
        public TokenInvalidException(String message) {
            super(message);
        }
    }
}

