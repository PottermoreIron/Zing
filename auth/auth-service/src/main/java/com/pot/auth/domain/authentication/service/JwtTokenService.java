package com.pot.auth.domain.authentication.service;

import com.pot.auth.domain.authentication.valueobject.JwtToken;
import com.pot.auth.domain.authentication.valueobject.RefreshToken;
import com.pot.auth.domain.authentication.valueobject.TokenPair;
import com.pot.auth.domain.authorization.service.PermissionDomainService;
import com.pot.auth.domain.authorization.valueobject.PermissionCacheMetadata;
import com.pot.auth.domain.authorization.valueobject.PermissionVersion;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.port.TokenManagementPort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.TokenId;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Set;

@Slf4j
public class JwtTokenService {

    private final TokenManagementPort tokenManagementPort;
    private final CachePort cachePort;
    private final UserModulePortFactory userModulePortFactory;
    private final PermissionDomainService permissionDomainService;
    private final long refreshTokenTtl;
    private final long refreshTokenSlidingWindow;
    private final boolean permissionVersionEnabled;

    public JwtTokenService(
            TokenManagementPort tokenManagementPort,
            CachePort cachePort,
            UserModulePortFactory userModulePortFactory,
            PermissionDomainService permissionDomainService,
            long refreshTokenTtl,
            long refreshTokenSlidingWindow,
            boolean permissionVersionEnabled) {
        this.tokenManagementPort = tokenManagementPort;
        this.cachePort = cachePort;
        this.userModulePortFactory = userModulePortFactory;
        this.permissionDomainService = permissionDomainService;
        this.refreshTokenTtl = refreshTokenTtl;
        this.refreshTokenSlidingWindow = refreshTokenSlidingWindow;
        this.permissionVersionEnabled = permissionVersionEnabled;
    }

    public TokenPair generateTokenPair(
            UserId userId,
            UserDomain userDomain,
            String nickname,
            Set<String> permissions) {
        Set<String> safePermissions = permissions != null ? permissions : Set.of();
        log.info("[Token] 生成Token对: userId={}, userDomain={}, nickname={}, permCount={}",
                userId, userDomain, nickname, safePermissions.size());

        try {
            PermissionCacheMetadata metadata = permissionDomainService.cachePermissionsWithMetadata(
                    userId,
                    userDomain,
                    safePermissions);

            log.debug("[Token] 权限已缓存: userId={}, version={}, digest={}",
                    userId, metadata.version(), metadata.digest());

            TokenPair tokenPair = tokenManagementPort.generateTokenPair(
                    userId,
                    userDomain,
                    nickname,
                    safePermissions,
                    metadata);

            storeRefreshToken(tokenPair.refreshToken());

            log.info("[Token] Token对生成成功: userId={}, accessTokenId={}, refreshTokenId={}",
                    userId, tokenPair.accessToken().tokenId(), tokenPair.refreshToken().tokenId());

            return tokenPair;

        } catch (Exception e) {
            log.error("[Token] Token生成失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new DomainException(AuthResultCode.AUTHENTICATION_FAILED);
        }
    }

    public JwtToken validateAccessToken(String tokenString) {
        log.debug("[Token] 验证AccessToken");

        JwtToken token = tokenManagementPort.parseAccessToken(tokenString);

        if (token.isExpired()) {
            log.warn("[Token] AccessToken已过期: tokenId={}", token.tokenId());
            throw new TokenExpiredException("AccessToken已过期");
        }

        if (isInBlacklist(token.tokenId())) {
            log.warn("[Token] AccessToken在黑名单中: tokenId={}", token.tokenId());
            throw new TokenInvalidException("Token已失效");
        }

        if (permissionVersionEnabled) {
            validatePermissionVersion(token);
        }

        log.debug("[Token] AccessToken验证成功: tokenId={}", token.tokenId());
        return token;
    }

    private void validatePermissionVersion(JwtToken token) {
        try {
            Long tokenPermVersion = token.getClaim("perm_version", Long.class);
            if (tokenPermVersion == null) {
                log.debug("[权限验证] Token无版本号（旧Token），跳过验证: tokenId={}", token.tokenId());
                return;
            }

            PermissionVersion currentVersion = permissionDomainService.getCurrentPermissionVersion(
                    token.userId(),
                    token.userDomain());

            PermissionVersion tokenVersion = new PermissionVersion(tokenPermVersion);
            if (tokenVersion.isOlderThan(currentVersion)) {
                log.warn("[权限验证] Token权限版本过期: userId={}, tokenVersion={}, currentVersion={}",
                        token.userId(), tokenVersion, currentVersion);
                throw new TokenInvalidException("权限已变更，请重新登录");
            }

            log.debug("[权限验证] 权限版证通过: userId={}, version={}",
                    token.userId(), tokenVersion);

        } catch (TokenInvalidException e) {
            throw e;
        } catch (Exception e) {
            log.error("[权限验证] 版本验证失败（降级放行）: error={}", e.getMessage());
        }
    }

    public TokenPair refreshToken(String refreshTokenString) {
        log.info("[Token] 开始刷新Token");

        RefreshToken oldRefreshToken = tokenManagementPort.parseRefreshToken(refreshTokenString);

        if (oldRefreshToken.isExpired()) {
            log.warn("[Token] RefreshToken已过期: tokenId={}", oldRefreshToken.tokenId());
            throw new TokenExpiredException("RefreshToken已过期，请重新登录");
        }

        String cacheKey = "auth:refresh:" + oldRefreshToken.tokenId().value();
        if (!cachePort.exists(cacheKey)) {
            log.warn("[Token] RefreshToken不存在或已被撤销: tokenId={}", oldRefreshToken.tokenId());
            throw new TokenInvalidException("RefreshToken已失效，请重新登录");
        }

        Set<String> authorities = userModulePortFactory
                .getPort(oldRefreshToken.userDomain())
                .getPermissions(oldRefreshToken.userId());

        PermissionCacheMetadata metadata = permissionDomainService.cachePermissionsWithMetadata(
                oldRefreshToken.userId(),
                oldRefreshToken.userDomain(),
                authorities);

        TokenPair newTokenPair = tokenManagementPort.generateTokenPair(
                oldRefreshToken.userId(),
                oldRefreshToken.userDomain(),
                getNicknameFromCache(oldRefreshToken),
                authorities,
                metadata);

        cachePort.delete(cacheKey);

        if (oldRefreshToken.isWithinSlidingWindow(refreshTokenSlidingWindow)) {
            log.info("[Token] RefreshToken在滑动窗口内，已续期: tokenId={}", oldRefreshToken.tokenId());
            storeRefreshToken(newTokenPair.refreshToken());
        } else {
            log.info("[Token] RefreshToken不在滑动窗口内，复用旧Token: tokenId={}", oldRefreshToken.tokenId());
            storeRefreshToken(oldRefreshToken);
        }

        log.info("[Token] Token刷新成功: userId={}", oldRefreshToken.userId());
        return newTokenPair;
    }

    public void addToBlacklist(TokenId tokenId, long remainingSeconds) {
        log.info("[Token] 将Token加入黑名单: tokenId={}, ttl={}s", tokenId, remainingSeconds);

        String blacklistKey = "auth:blacklist:" + tokenId.value();
        cachePort.set(blacklistKey, "1", Duration.ofSeconds(remainingSeconds));
    }

    public void logout(String accessTokenStr, String refreshTokenStr) {
        log.info("[Token] 执行登出");

        try {
            JwtToken token = tokenManagementPort.parseAccessToken(accessTokenStr);
            long remaining = token.getRemainingSeconds();
            if (remaining > 0) {
                addToBlacklist(token.tokenId(), remaining);
                log.info("[Token] AccessToken 已加入黑名单: tokenId={}, ttl={}s",
                        token.tokenId(), remaining);
            } else {
                log.debug("[Token] AccessToken 已自然过期，无需加入黑名单");
            }
        } catch (Exception e) {
            log.warn("[Token] 登出时 AccessToken 解析失败（忽略）: {}", e.getMessage());
        }

        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            try {
                RefreshToken refreshToken = tokenManagementPort.parseRefreshToken(refreshTokenStr);
                String cacheKey = "auth:refresh:" + refreshToken.tokenId().value();
                cachePort.delete(cacheKey);
                log.info("[Token] RefreshToken 缓存已删除: tokenId={}", refreshToken.tokenId());
            } catch (Exception e) {
                log.warn("[Token] 登出时 RefreshToken 解析失败（忽略）: {}", e.getMessage());
            }
        }

        log.info("[Token] 登出完成");
    }

    private boolean isInBlacklist(TokenId tokenId) {
        String blacklistKey = "auth:blacklist:" + tokenId.value();
        return cachePort.exists(blacklistKey);
    }

    private void storeRefreshToken(RefreshToken refreshToken) {
        String cacheKey = "auth:refresh:" + refreshToken.tokenId().value();
        long ttl = Math.min(refreshToken.getRemainingSeconds(), refreshTokenTtl);
        cachePort.set(cacheKey, refreshToken.rawToken(), Duration.ofSeconds(ttl));
    }

    private String getNicknameFromCache(RefreshToken refreshToken) {
        return userModulePortFactory.getPort(refreshToken.userDomain())
                .findById(refreshToken.userId())
                .map(user -> user.nickname())
                .orElse("unknown");
    }

    public static class TokenExpiredException extends DomainException {
        public TokenExpiredException(String message) {
            super(message);
        }
    }

    public static class TokenInvalidException extends DomainException {
        public TokenInvalidException(String message) {
            super(message);
        }
    }
}
