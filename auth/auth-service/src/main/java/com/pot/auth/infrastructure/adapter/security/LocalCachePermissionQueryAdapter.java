package com.pot.auth.infrastructure.adapter.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pot.auth.domain.authorization.service.PermissionDomainService;
import com.pot.auth.domain.port.PermissionQueryPort;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import com.pot.auth.domain.authorization.constant.CacheKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Permission query adapter with a short-lived local cache.
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
            log.debug("[Permission] L1 hit — userId={}, userDomain={}", userId, userDomain);
            return l1Permissions;
        }

        Set<String> permissions = permissionDomainService.getCachedPermissions(userId, userDomain);
        if (permissions.isEmpty()) {
            log.warn("[Permission] L2 fallback (no cache) — userId={}, userDomain={}", userId, userDomain);
            return Set.of();
        }

        localCache.put(cacheKey, permissions);
        log.debug("[Permission] L2 hit, promoting to L1 — userId={}, userDomain={}, permCount={}",
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