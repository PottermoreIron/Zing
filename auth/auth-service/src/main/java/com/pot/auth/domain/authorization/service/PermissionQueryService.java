package com.pot.auth.domain.authorization.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pot.auth.infrastructure.constant.CacheKeyConstants;
import com.pot.auth.domain.port.CachePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * 多级权限查询服务
 *
 * <p>
 * 提供多级缓存策略的权限查询：
 * <ul>
 * <li>L1: Caffeine本地缓存（5分钟TTL）</li>
 * <li>L2: Redis分布式缓存（1小时TTL）</li>
 * <li>L3: 数据库查询（降级策略）</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Service
public class PermissionQueryService {

    private final CachePort cachePort;
    private final Cache<String, Set<String>> localCache;

    public PermissionQueryService(CachePort cachePort) {
        this.cachePort = cachePort;
        // 初始化本地缓存：5分钟过期，最多缓存10000个用户
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(10000)
                .recordStats()
                .build();
    }

    /**
     * 查询用户权限（多级缓存）
     *
     * @param namespace 命名空间（如 "member", "admin"）
     * @param userId    用户ID
     * @return 用户权限集合，如果未找到返回空集合
     */
    public Set<String> queryUserPermissions(String namespace, String userId) {
        String cacheKey = CacheKeyConstants.buildPermissionKey(namespace, userId);

        // L1: 查询本地缓存
        Set<String> l1Permissions = localCache.getIfPresent(cacheKey);
        if (l1Permissions != null) {
            log.debug("[权限查询] L1命中: namespace={}, userId={}", namespace, userId);
            return l1Permissions;
        }

        // L2: 查询Redis缓存
        @SuppressWarnings("rawtypes")
        Optional<Set> l2PermissionsOpt = cachePort.get(cacheKey, Set.class);
        if (l2PermissionsOpt.isPresent()) {
            @SuppressWarnings("unchecked")
            Set<String> cachedPermissions = (Set<String>) l2PermissionsOpt.get();
            log.debug("[权限查询] L2命中: namespace={}, userId={}", namespace, userId);
            localCache.put(cacheKey, cachedPermissions);
            return cachedPermissions;
        }

        // L3: 查询数据库（降级策略）
        log.warn("[权限查询] 缓存未命中，走降级策略: namespace={}, userId={}", namespace, userId);
        // TODO: 从member-service查询权限（通过Feign）
        return Set.of();
    }

    /**
     * 刷新本地缓存
     *
     * @param namespace 命名空间
     * @param userId    用户ID
     */
    public void refreshLocalCache(String namespace, String userId) {
        String cacheKey = CacheKeyConstants.buildPermissionKey(namespace, userId);
        localCache.invalidate(cacheKey);
        log.debug("[权限查询] 刷新本地缓存: namespace={}, userId={}", namespace, userId);
    }

    /**
     * 清空本地缓存
     */
    public void clearLocalCache() {
        localCache.invalidateAll();
        log.info("[权限查询] 清空所有本地缓存");
    }

    /**
     * 获取本地缓存统计信息
     */
    public String getCacheStats() {
        return localCache.stats().toString();
    }
}
