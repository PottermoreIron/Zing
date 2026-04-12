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
        log.info("[Token] Generating token pair — userId={}, userDomain={}, nickname={}, permCount={}",
                userId, userDomain, nickname, safePermissions.size());

        try {
            PermissionCacheMetadata metadata = permissionDomainService.cachePermissionsWithMetadata(
                    userId,
                    userDomain,
                    safePermissions);

            log.debug("[Token] Permissions cached — userId={}, version={}, digest={}",
                    userId, metadata.version(), metadata.digest());

            TokenPair tokenPair = tokenManagementPort.generateTokenPair(
                    userId,
                    userDomain,
                    nickname,
                    safePermissions,
                    metadata);

            storeRefreshToken(tokenPair.refreshToken());

            log.info("[Token] Token pair generated — userId={}, accessTokenId={}, refreshTokenId={}",
                    userId, tokenPair.accessToken().tokenId(), tokenPair.refreshToken().tokenId());

            return tokenPair;

        } catch (Exception e) {
            log.error("[Token] Failed to generate token pair — userId={}, error={}", userId, e.getMessage(), e);
            throw new DomainException(AuthResultCode.AUTHENTICATION_FAILED);
        }
    }

    public JwtToken validateAccessToken(String tokenString) {
        log.debug("[Token] Validating AccessToken");

        JwtToken token = tokenManagementPort.parseAccessToken(tokenString);

        if (token.isExpired()) {
            log.warn("[Token] AccessToken has expired: tokenId={}", token.tokenId());
            throw new TokenExpiredException("AccessToken has expired");
        }

        if (isInBlacklist(token.tokenId())) {
            log.warn("[Token] AccessToken is blacklisted — tokenId={}", token.tokenId());
            throw new TokenInvalidException("Token has been revoked");
        }

        if (permissionVersionEnabled) {
            validatePermissionVersion(token);
        }

        log.debug("[Token] AccessToken validation passed — tokenId={}", token.tokenId());
        return token;
    }

    private void validatePermissionVersion(JwtToken token) {
        try {
            Long tokenPermVersion = token.getClaim("perm_version", Long.class);
            if (tokenPermVersion == null) {
                log.debug("[PermVerify] Token has no version (legacy token), skipping verification — tokenId={}", token.tokenId());
                return;
            }

            PermissionVersion currentVersion = permissionDomainService.getCurrentPermissionVersion(
                    token.userId(),
                    token.userDomain());

            PermissionVersion tokenVersion = new PermissionVersion(tokenPermVersion);
            if (tokenVersion.isOlderThan(currentVersion)) {
                log.warn("[PermVerify] Token permission version stale — userId={}, tokenVersion={}, currentVersion={}",
                        token.userId(), tokenVersion, currentVersion);
                throw new TokenInvalidException("Permissions have changed, please sign in again");
            }

            log.debug("[PermVerify] Permission version verified — userId={}, version={}",
                    token.userId(), tokenVersion);

        } catch (TokenInvalidException e) {
            throw e;
        } catch (Exception e) {
            log.error("[PermVerify] Version verification failed (degraded, allowing through) — error={}", e.getMessage());
        }
    }

    public TokenPair refreshToken(String refreshTokenString) {
        log.info("[Token] Refreshing token");

        RefreshToken oldRefreshToken = tokenManagementPort.parseRefreshToken(refreshTokenString);

        if (oldRefreshToken.isExpired()) {
            log.warn("[Token] RefreshToken has expired: tokenId={}", oldRefreshToken.tokenId());
            throw new TokenExpiredException("Refresh token has expired, please sign in again");
        }

        String cacheKey = "auth:refresh:" + oldRefreshToken.tokenId().value();
        if (!cachePort.exists(cacheKey)) {
            log.warn("[Token] RefreshToken does not exist or has been revoked — tokenId={}", oldRefreshToken.tokenId());
            throw new TokenInvalidException("Refresh token has been revoked, please sign in again");
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
            log.info("[Token] RefreshToken renewed within sliding window — tokenId={}", oldRefreshToken.tokenId());
            storeRefreshToken(newTokenPair.refreshToken());
        } else {
            log.info("[Token] RefreshToken outside sliding window, reusing existing token — tokenId={}", oldRefreshToken.tokenId());
            storeRefreshToken(oldRefreshToken);
        }

        log.info("[Token] Token refreshed — userId={}", oldRefreshToken.userId());
        return newTokenPair;
    }

    public void addToBlacklist(TokenId tokenId, long remainingSeconds) {
        log.info("[Token] Blacklisting token — tokenId={}, ttl={}s", tokenId, remainingSeconds);

        String blacklistKey = "auth:blacklist:" + tokenId.value();
        cachePort.set(blacklistKey, "1", Duration.ofSeconds(remainingSeconds));
    }

    public void logout(String accessTokenStr, String refreshTokenStr) {
        log.info("[Token] Processing logout");

        try {
            JwtToken token = tokenManagementPort.parseAccessToken(accessTokenStr);
            long remaining = token.getRemainingSeconds();
            if (remaining > 0) {
                addToBlacklist(token.tokenId(), remaining);
                log.info("[Token] AccessToken blacklisted — tokenId={}, ttl={}s",
                        token.tokenId(), remaining);
            } else {
                log.debug("[Token] AccessToken already expired naturally, blacklist not required");
            }
        } catch (Exception e) {
            log.warn("[Token] Failed to parse AccessToken during logout (ignored): {}", e.getMessage());
        }

        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            try {
                RefreshToken refreshToken = tokenManagementPort.parseRefreshToken(refreshTokenStr);
                String cacheKey = "auth:refresh:" + refreshToken.tokenId().value();
                cachePort.delete(cacheKey);
                log.info("[Token] RefreshToken cache entry removed — tokenId={}", refreshToken.tokenId());
            } catch (Exception e) {
                log.warn("[Token] Failed to parse RefreshToken during logout (ignored): {}", e.getMessage());
            }
        }

        log.info("[Token] Logout complete");
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
