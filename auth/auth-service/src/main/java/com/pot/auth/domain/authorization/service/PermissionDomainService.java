package com.pot.auth.domain.authorization.service;

import com.pot.auth.domain.authorization.valueobject.PermissionCacheMetadata;
import com.pot.auth.domain.authorization.valueobject.PermissionDigest;
import com.pot.auth.domain.authorization.valueobject.PermissionVersion;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import com.pot.auth.domain.authorization.constant.CacheKeyConstants;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class PermissionDomainService {

    private final CachePort cachePort;
    private final long permissionCacheTtl;

    public PermissionDomainService(CachePort cachePort, long permissionCacheTtl) {
        this.cachePort = cachePort;
        this.permissionCacheTtl = permissionCacheTtl;
    }

    public PermissionCacheMetadata cachePermissionsWithMetadata(
            UserId userId,
            UserDomain userDomain,
            Set<String> permissions) {
        log.debug("[权限缓存] 开始缓存权限: userId={}, userDomain={}, permCount={}",
                userId, userDomain, permissions.size());

        PermissionDigest digest = PermissionDigest.from(permissions);
        log.debug("[权限缓存] 权限摘要计算完成: userId={}, digest={}",
                userId, digest.shortValue());

        PermissionVersion version = incrementPermissionVersion(userId, userDomain);
        log.debug("[权限缓存] 权限版本号: userId={}, version={}", userId, version);

        cachePermissions(userId, userDomain, permissions);

        cachePermissionDigest(userId, userDomain, digest);

        return new PermissionCacheMetadata(version.value(), digest.value());
    }

    public void incrementPermissionVersion(String namespace, String userId) {
        try {
            incrementPermissionVersion(
                    new UserId(Long.parseLong(userId)),
                    UserDomain.fromCode(namespace));
        } catch (Exception e) {
            log.error("[权限版本] 递增版本号失败: namespace={}, userId={}, error={}",
                    namespace, userId, e.getMessage(), e);
        }
    }

    public PermissionVersion incrementPermissionVersion(UserId userId, UserDomain userDomain) {
        String versionKey = CacheKeyConstants.buildPermissionVersionKey(
                userDomain.getCode(),
                userId.value().toString());

        try {
            long newVersion = cachePort.increment(versionKey, 1, Duration.ZERO);

            log.debug("[权限版本] 版本号递增: userId={}, userDomain={}, newVersion={}",
                    userId, userDomain, newVersion);

            return new PermissionVersion(newVersion);

        } catch (Exception e) {
            log.error("[权限版本] 版本号递增失败，使用初始版本号: userId={}, error={}",
                    userId, e.getMessage());
            return PermissionVersion.initial();
        }
    }

    public PermissionVersion getCurrentPermissionVersion(UserId userId, UserDomain userDomain) {
        String versionKey = CacheKeyConstants.buildPermissionVersionKey(
                userDomain.getCode(),
                userId.value().toString());

        try {
            Optional<Long> versionOpt = cachePort.get(versionKey, Long.class);
            if (versionOpt.isPresent()) {
                return new PermissionVersion(versionOpt.get());
            }
        } catch (Exception e) {
            log.warn("[权限版本] 获取版本号失败: userId={}, error={}", userId, e.getMessage());
        }

        return PermissionVersion.initial();
    }

    private void cachePermissions(
            UserId userId,
            UserDomain userDomain,
            Set<String> permissions) {
        String cacheKey = CacheKeyConstants.buildPermissionKey(
                userDomain.getCode(),
                userId.value().toString());
        Duration ttl = Duration.ofSeconds(permissionCacheTtl);

        try {
            Set<String> permsToCache = permissions.isEmpty()
                    ? Collections.singleton("__EMPTY__")
                    : permissions;

            cachePort.set(cacheKey, permsToCache, ttl);

            log.info("[权限缓存] 权限已缓存到Redis: userId={}, userDomain={}, permCount={}, ttl={}s",
                    userId, userDomain, permissions.size(), permissionCacheTtl);

        } catch (Exception e) {
            log.error("[权限缓存] Redis缓存失败（非致命错误）: userId={}, error={}",
                    userId, e.getMessage());
        }
    }

    private void cachePermissionDigest(
            UserId userId,
            UserDomain userDomain,
            PermissionDigest digest) {
        String digestKey = CacheKeyConstants.buildPermissionDigestKey(
                userDomain.getCode(),
                userId.value().toString());
        Duration ttl = Duration.ofSeconds(permissionCacheTtl);

        try {
            cachePort.set(digestKey, digest.value(), ttl);
            log.debug("[权限缓存] 权限摘要已缓存: userId={}, digest={}",
                    userId, digest.shortValue());
        } catch (Exception e) {
            log.warn("[权限缓存] 摘要缓存失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    public Set<String> getCachedPermissions(UserId userId, UserDomain userDomain) {
        String cacheKey = CacheKeyConstants.buildPermissionKey(
                userDomain.getCode(),
                userId.value().toString());

        try {
            @SuppressWarnings("rawtypes")
            Optional<Set> permsOpt = cachePort.get(cacheKey, Set.class);
            if (permsOpt.isPresent()) {
                @SuppressWarnings("unchecked")
                Set<String> perms = (Set<String>) permsOpt.get();

                if (perms.size() == 1 && perms.contains("__EMPTY__")) {
                    return Collections.emptySet();
                }

                log.debug("[权限缓存] 命中缓存: userId={}, permCount={}", userId, perms.size());
                return perms;
            }
        } catch (Exception e) {
            log.warn("[权限缓存] 获取缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        return Collections.emptySet();
    }

    public void invalidatePermissionCache(String namespace, String userId) {
        try {
            invalidatePermissionCache(
                    new UserId(Long.parseLong(userId)),
                    UserDomain.fromCode(namespace));
        } catch (Exception e) {
            log.error("[权限缓存] 失效缓存失败: namespace={}, userId={}, error={}",
                    namespace, userId, e.getMessage(), e);
        }
    }

    public void invalidatePermissionCache(UserId userId, UserDomain userDomain) {
        String permKey = CacheKeyConstants.buildPermissionKey(
                userDomain.getCode(),
                userId.value().toString());
        String digestKey = CacheKeyConstants.buildPermissionDigestKey(
                userDomain.getCode(),
                userId.value().toString());

        try {
            cachePort.delete(permKey);
            cachePort.delete(digestKey);
            log.info("[权限缓存] 缓存已清除: userId={}, userDomain={}", userId, userDomain);
        } catch (Exception e) {
            log.error("[权限缓存] 清除缓存失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    public boolean verifyPermissionDigest(
            UserId userId,
            UserDomain userDomain,
            Set<String> permissions) {
        String digestKey = CacheKeyConstants.buildPermissionDigestKey(
                userDomain.getCode(),
                userId.value().toString());

        try {
            Optional<String> cachedDigestOpt = cachePort.get(digestKey, String.class);
            if (cachedDigestOpt.isEmpty()) {
                log.debug("[权限验证] 无缓存摘要，跳过验证: userId={}", userId);
                return true;
            }

            PermissionDigest cachedDigest = new PermissionDigest(cachedDigestOpt.get());
            PermissionDigest currentDigest = PermissionDigest.from(permissions);

            boolean matches = cachedDigest.equals(currentDigest);
            if (!matches) {
                log.warn("[权限验证] 摘要不匹配: userId={}, cached={}, current={}",
                        userId, cachedDigest.shortValue(), currentDigest.shortValue());
            }
            return matches;

        } catch (Exception e) {
            log.error("[权限验证] 摘要验证失败（降级放行）: userId={}, error={}",
                    userId, e.getMessage());
            return true;
        }
    }
}
