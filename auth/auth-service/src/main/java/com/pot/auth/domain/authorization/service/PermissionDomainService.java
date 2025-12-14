package com.pot.auth.domain.authorization.service;

import com.pot.auth.domain.authorization.valueobject.PermissionCacheMetadata;
import com.pot.auth.domain.authorization.valueobject.PermissionDigest;
import com.pot.auth.domain.authorization.valueobject.PermissionVersion;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import com.pot.auth.infrastructure.constant.CacheKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * 权限领域服务
 *
 * <p>
 * 负责权限的缓存、版本管理和摘要计算
 *
 * <p>
 * 核心功能：
 * <ul>
 * <li>权限缓存：将权限集合缓存到Redis</li>
 * <li>版本管理：递增权限版本号，实现Token实时失效</li>
 * <li>摘要计算：计算权限摘要，用于防篡改验证</li>
 * <li>缓存失效：清除用户权限缓存</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionDomainService {

    private final CachePort cachePort;

    @Value("${auth.permission.cache.ttl:3600}")
    private long permissionCacheTtl; // 权限缓存TTL（默认1小时）

    /**
     * 缓存权限并生成元数据
     *
     * <p>
     * 实现策略：
     * <ol>
     * <li>计算权限摘要（MD5）</li>
     * <li>获取/递增权限版本号</li>
     * <li>缓存权限到Redis</li>
     * <li>缓存权限摘要</li>
     * </ol>
     *
     * @param userId      用户ID
     * @param userDomain  用户域
     * @param permissions 权限集合
     * @return 权限缓存元数据（版本号 + 摘要）
     */
    public PermissionCacheMetadata cachePermissionsWithMetadata(
            UserId userId,
            UserDomain userDomain,
            Set<String> permissions) {
        log.debug("[权限缓存] 开始缓存权限: userId={}, userDomain={}, permCount={}",
                userId, userDomain, permissions.size());

        // 1. 计算权限摘要
        PermissionDigest digest = PermissionDigest.from(permissions);
        log.debug("[权限缓存] 权限摘要计算完成: userId={}, digest={}",
                userId, digest.shortValue());

        // 2. 递增权限版本号
        PermissionVersion version = incrementPermissionVersion(userId, userDomain);
        log.debug("[权限缓存] 权限版本号: userId={}, version={}", userId, version);

        // 3. 缓存权限
        cachePermissions(userId, userDomain, permissions);

        // 4. 缓存权限摘要
        cachePermissionDigest(userId, userDomain, digest);

        // 5. 返回元数据
        return new PermissionCacheMetadata(version.value(), digest.value());
    }

    /**
     * 递增权限版本号（String参数版本，用于MQ监听器）
     */
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

    /**
     * 递增权限版本号
     *
     * <p>
     * 权限版本号存储在Redis，永久有效
     * <p>
     * 当权限变更时，版本号递增，导致旧Token失效
     *
     * @param userId     用户ID
     * @param userDomain 用户域
     * @return 新版本号
     */
    public PermissionVersion incrementPermissionVersion(UserId userId, UserDomain userDomain) {
        String versionKey = CacheKeyConstants.buildPermissionVersionKey(
                userDomain.getCode(),
                userId.value().toString());

        try {
            // 递增版本号（初始值为1，永久有效）
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

    /**
     * 获取当前权限版本号
     *
     * @param userId     用户ID
     * @param userDomain 用户域
     * @return 权限版本号，如果不存在则返回初始版本号
     */
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

    /**
     * 缓存权限
     *
     * <p>
     * Key格式：auth:perms:{userDomain}:{userId}
     * <p>
     * TTL：可配置（默认1小时）
     *
     * @param userId      用户ID
     * @param userDomain  用户域
     * @param permissions 权限集合
     */
    private void cachePermissions(
            UserId userId,
            UserDomain userDomain,
            Set<String> permissions) {
        String cacheKey = CacheKeyConstants.buildPermissionKey(
                userDomain.getCode(),
                userId.value().toString());
        Duration ttl = Duration.ofSeconds(permissionCacheTtl);

        try {
            // 空权限也要缓存（防止缓存穿透）
            Set<String> permsToCache = permissions.isEmpty()
                    ? Collections.singleton("__EMPTY__")
                    : permissions;

            cachePort.set(cacheKey, permsToCache, ttl);

            log.info("[权限缓存] 权限已缓存到Redis: userId={}, userDomain={}, permCount={}, ttl={}s",
                    userId, userDomain, permissions.size(), permissionCacheTtl);

        } catch (Exception e) {
            log.error("[权限缓存] Redis缓存失败（非致命错误）: userId={}, error={}",
                    userId, e.getMessage());
            // 不抛出异常，允许降级
        }
    }

    /**
     * 缓存权限摘要
     *
     * <p>
     * 用于快速验证Token中的权限摘要是否匹配
     *
     * @param userId     用户ID
     * @param userDomain 用户域
     * @param digest     权限摘要
     */
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

    /**
     * 获取缓存的权限集合
     *
     * @param userId     用户ID
     * @param userDomain 用户域
     * @return 权限集合，如果不存在则返回空集合
     */
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

                // 过滤空权限标记
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

    /**
     * 失效权限缓存（String参数版本，用于MQ监听器）
     */
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

    /**
     * 清除用户权限缓存
     *
     * <p>
     * 用于权限变更后强制刷新缓存
     *
     * @param userId     用户ID
     * @param userDomain 用户域
     */
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

    /**
     * 验证权限摘要
     *
     * @param userId      用户ID
     * @param userDomain  用户域
     * @param permissions 权限集合
     * @return true if摘要匹配
     */
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
                // 降级策略：无缓存时放行
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
            // 降级策略：验证失败时放行
            return true;
        }
    }
}
