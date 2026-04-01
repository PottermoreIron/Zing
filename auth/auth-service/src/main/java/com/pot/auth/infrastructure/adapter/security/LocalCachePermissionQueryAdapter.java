package com.pot.auth.infrastructure.adapter.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pot.auth.domain.authorization.service.PermissionDomainService;
import com.pot.auth.domain.port.PermissionQueryPort;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import com.pot.auth.infrastructure.constant.CacheKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * 本地缓存权限查询适配器
 *
 * <p>
 * 适配 {@link PermissionQueryPort}，提供两级读取策略：
 * <ul>
 * <li>L1: Caffeine 本地缓存</li>
 * <li>L2: 通过 {@link PermissionDomainService} 读取 Redis 中的权限缓存</li>
 * </ul>
 *
 * <p>
 * 领域服务负责权限缓存的语义和降级策略，适配器仅负责本地缓存加速。
 */
@Slf4j
@Component
public class LocalCachePermissionQueryAdapter implements PermissionQueryPort {

    private final PermissionDomainService permissionDomainService;
    private final Cache<String, Set<String>> localCache;

    public LocalCachePermissionQueryAdapter(PermissionDomainService permissionDomainService) {
        this.permissionDomainService = permissionDomainService;
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(10000)
                .recordStats()
                .build();
    }

    @Override
    public Set<String> getCachedPermissions(UserId userId, UserDomain userDomain) {
        String cacheKey = CacheKeyConstants.buildPermissionKey(
                userDomain.getCode(),
                userId.value().toString());

        Set<String> l1Permissions = localCache.getIfPresent(cacheKey);
        if (l1Permissions != null) {
            log.debug("[权限查询] L1命中: userId={}, userDomain={}", userId, userDomain);
            return l1Permissions;
        }

        Set<String> permissions = permissionDomainService.getCachedPermissions(userId, userDomain);
        if (permissions.isEmpty()) {
            log.warn("[权限查询] 缓存未命中（降级）: userId={}, userDomain={}", userId, userDomain);
            return Set.of();
        }

        localCache.put(cacheKey, permissions);
        log.debug("[权限查询] L2命中并写入L1: userId={}, userDomain={}, permCount={}",
                userId, userDomain, permissions.size());
        return permissions;
    }

    public void invalidateLocalCache(UserId userId, UserDomain userDomain) {
        localCache.invalidate(CacheKeyConstants.buildPermissionKey(
                userDomain.getCode(),
                userId.value().toString()));
    }

    public void clearLocalCache() {
        localCache.invalidateAll();
    }

    public String getCacheStats() {
        return localCache.stats().toString();
    }
}