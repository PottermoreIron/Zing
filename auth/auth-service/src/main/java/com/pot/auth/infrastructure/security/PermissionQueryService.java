package com.pot.auth.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.infrastructure.constant.CacheKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * 多级权限查询服务（基础设施层）
 *
 * <p>
 * 提供多级缓存策略的权限查询：
 * <ul>
 * <li>L1: Caffeine本地缓存（5分钟TTL）</li>
 * <li>L2: Redis分布式缓存</li>
 * <li>L3: 降级策略（缓存未命中时返回空集合，等待下次同步）</li>
 * </ul>
 *
 * @author pot
 * @since 2026-03-22
 */
@Slf4j
@Service
public class PermissionQueryService {

    private final CachePort cachePort;
    private final Cache<String, Set<String>> localCache;

    public PermissionQueryService(CachePort cachePort) {
        this.cachePort = cachePort;
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(10000)
                .recordStats()
                .build();
    }

    /**
     * 查询用户权限（L1 → L2 → 降级）
     */
    public Set<String> queryUserPermissions(String namespace, String userId) {
        String cacheKey = CacheKeyConstants.buildPermissionKey(namespace, userId);

        // L1: 本地缓存
        Set<String> l1 = localCache.getIfPresent(cacheKey);
        if (l1 != null) {
            log.debug("[权限查询] L1命中: namespace={}, userId={}", namespace, userId);
            return l1;
        }

        // L2: Redis
        @SuppressWarnings("rawtypes")
        Optional<Set> l2Opt = cachePort.get(cacheKey, Set.class);
        if (l2Opt.isPresent()) {
            @SuppressWarnings("unchecked")
            Set<String> cached = (Set<String>) l2Opt.get();
            log.debug("[权限查询] L2命中: namespace={}, userId={}", namespace, userId);
            localCache.put(cacheKey, cached);
            return cached;
        }

        log.warn("[权限查询] 缓存未命中（降级）: namespace={}, userId={}", namespace, userId);
        return Set.of();
    }

    public void refreshLocalCache(String namespace, String userId) {
        localCache.invalidate(CacheKeyConstants.buildPermissionKey(namespace, userId));
    }

    public void clearLocalCache() {
        localCache.invalidateAll();
    }

    public String getCacheStats() {
        return localCache.stats().toString();
    }
}
