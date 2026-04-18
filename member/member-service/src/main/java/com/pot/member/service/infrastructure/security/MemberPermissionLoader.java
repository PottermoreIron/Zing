package com.pot.member.service.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pot.member.service.application.service.MemberPermissionApplicationService;
import com.pot.zing.framework.starter.security.port.PermissionLoaderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

/**
 * Loads user permissions directly from the local domain service and caches
 * them.
 *
 * <p>
 * Uses Caffeine as a local in-process cache keyed by
 * {@code domain:userId:permVersion}. When the gateway increments
 * {@code X-Perm-Version}, the cache key changes and the stale entry is
 * naturally evicted by the size-based eviction policy.
 * </p>
 *
 * <p>
 * This implementation intentionally does <em>not</em> call auth-service.
 * member-service owns its own permission data; going back to auth-service would
 * create a circular service dependency (auth → member at login, member → auth
 * at
 * request time).
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPermissionLoader implements PermissionLoaderPort {

    private static final int MAX_CACHE_SIZE = 5_000;
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final MemberPermissionApplicationService memberPermissionApplicationService;

    private final Cache<String, Set<String>> permissionCache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterWrite(CACHE_TTL)
            .build();

    @Override
    public Set<String> loadPermissions(String userId, String userDomain, String permVersion) {
        String cacheKey = buildCacheKey(userId, userDomain, permVersion);
        Set<String> cached = permissionCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            Set<String> permissions = memberPermissionApplicationService.getPermissionCodes(Long.parseLong(userId));
            Set<String> immutable = Collections.unmodifiableSet(permissions);
            permissionCache.put(cacheKey, immutable);
            return immutable;
        } catch (Exception e) {
            log.warn("Failed to load permissions for userId={} domain={}: {}", userId, userDomain, e.getMessage());
            return Collections.emptySet();
        }
    }

    private String buildCacheKey(String userId, String userDomain, String permVersion) {
        return userDomain + ":" + userId + ":" + (permVersion != null ? permVersion : "");
    }
}
