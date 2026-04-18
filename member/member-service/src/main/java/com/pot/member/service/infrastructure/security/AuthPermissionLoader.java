package com.pot.member.service.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pot.member.service.infrastructure.client.AuthServiceClient;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.security.port.PermissionLoaderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

/**
 * Loads user permissions from auth-service and caches them locally.
 *
 * <p>Uses Caffeine as a local in-process cache keyed by
 * {@code domain:userId:permVersion}. When the gateway increments
 * {@code X-Perm-Version}, the cache key changes and the stale entry is
 * naturally evicted by the size-based eviction policy.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthPermissionLoader implements PermissionLoaderPort {

    private static final int MAX_CACHE_SIZE = 5_000;
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final AuthServiceClient authServiceClient;

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
            R<Set<String>> response = authServiceClient.getPermissions(Long.parseLong(userId), userDomain);
            Set<String> permissions = (response != null && !CollectionUtils.isEmpty(response.getData()))
                    ? Collections.unmodifiableSet(response.getData())
                    : Collections.emptySet();
            permissionCache.put(cacheKey, permissions);
            return permissions;
        } catch (Exception e) {
            log.warn("Failed to load permissions from auth-service for userId={} domain={}: {}",
                    userId, userDomain, e.getMessage());
            return Collections.emptySet();
        }
    }

    private String buildCacheKey(String userId, String userDomain, String permVersion) {
        return userDomain + ":" + userId + ":" + (permVersion != null ? permVersion : "");
    }
}
