package com.pot.auth.domain.port;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 缓存端口接口（防腐层）
 *
 * <p>领域层通过此接口访问缓存，不依赖具体的缓存实现（Redis/Caffeine）
 * <p>实现类：
 * <ul>
 *   <li>RedisCacheAdapter - Redis实现</li>
 *   <li>LocalCacheAdapter - Caffeine本地缓存实现</li>
 *   <li>CompositeCacheAdapter - L1+L2组合缓存实现</li>
 * </ul>
 *
 * @author pot
 * @since 1.0.0
 */
public interface CachePort {

    // ========== 基本操作 ==========

    /**
     * 设置缓存
     */
    <T> void set(String key, T value, Duration ttl);

    /**
     * 获取缓存
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * 删除缓存
     */
    void delete(String key);

    /**
     * 批量删除
     */
    void deleteBatch(Set<String> keys);

    /**
     * 是否存在
     */
    boolean exists(String key);

    // ========== 集合操作 ==========

    /**
     * 添加到集合
     */
    <T> void addToSet(String key, T value, Duration ttl);

    /**
     * 从集合删除
     */
    <T> void removeFromSet(String key, T value);

    /**
     * 获取集合所有元素
     */
    <T> Set<T> getSet(String key, Class<T> type);

    /**
     * 判断是否是集合成员
     */
    <T> boolean isMemberOfSet(String key, T value);

    // ========== Hash操作 ==========

    /**
     * 设置Hash字段
     */
    <T> void setHash(String key, String field, T value, Duration ttl);

    /**
     * 获取Hash字段
     */
    <T> Optional<T> getHash(String key, String field, Class<T> type);

    /**
     * 获取Hash所有字段
     */
    <T> Map<String, T> getAllHash(String key, Class<T> type);

    /**
     * 删除Hash字段
     */
    void deleteHash(String key, String field);

    // ========== 计数器操作 ==========

    /**
     * 递增
     */
    long increment(String key, long delta, Duration ttl);

    /**
     * 递减
     */
    long decrement(String key, long delta);

    // ========== 高级操作 ==========

    /**
     * 如果不存在则设置
     */
    <T> boolean setIfAbsent(String key, T value, Duration ttl);

    /**
     * 设置过期时间
     */
    void expire(String key, Duration ttl);

    /**
     * 获取剩余TTL
     */
    Optional<Duration> getTtl(String key);

    /**
     * 持久化（移除过期时间）
     */
    void persist(String key);
}

