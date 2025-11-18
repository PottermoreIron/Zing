package com.pot.zing.framework.starter.redis.service;

import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author: Pot
 * @created: 2025/10/18 20:53
 * @description: 自定义Redis接口类
 */
public interface RedisService {
    // ========== 基础操作 ==========

    /**
     * 设置键值对
     */
    Boolean set(String key, Object value);

    /**
     * 设置键值对，带过期时间
     */
    Boolean set(String key, Object value, Duration timeout);

    /**
     * 设置键值对，带过期时间
     */
    Boolean set(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 设置键值对，如果键不存在
     */
    Boolean setIfAbsent(String key, Object value);

    /**
     * 设置键值对，带过期时间，如果键不存在
     */
    Boolean setIfAbsent(String key, Object value, Duration timeout);

    /**
     * 设置键值对，带过期时间，如果键不存在
     */
    Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 获取值
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 删除键
     */
    Boolean delete(String key);

    /**
     * 批量删除键
     */
    Long delete(Collection<String> keys);

    /**
     * 检查键是否存在
     */
    Boolean exists(String key);

    /**
     * 设置过期时间
     */
    Boolean expire(String key, Duration timeout);

    /**
     * 设置过期时间
     */
    Boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * 获取剩余过期时间
     */
    Long getExpire(String key);

    /**
     * 获取剩余过期时间（返回Duration）
     */
    Duration getExpireDuration(String key);

    /**
     * 持久化键（移除过期时间）
     */
    Boolean persist(String key);

    /**
     * 获取键的类型
     */
    String type(String key);

    /**
     * 递增键的值
     */
    Long increment(String key, long delta);

    /**
     * 递增键的值, 默认增量为1
     */
    Long increment(String key);

    /**
     * 递增键的值，支持浮点数
     */
    Double increment(String key, double delta);

    // ========== Hash操作 ==========

    /**
     * 设置Hash键值对
     */
    Boolean hSet(String key, String field, Object value);

    Boolean hSetAll(String key, Map<String, Object> map);

    Boolean hSetIfAbsent(String key, String field, Object value);

    /**
     * 获取Hash键的值
     */

    <T> T hGet(String key, String field, Class<T> clazz);

    Map<Object, Object> hGetAll(String key);

    Long hDelete(String key, String... fields);

    Boolean hExists(String key, String field);

    Long hIncrement(String key, String field, long delta);

    Set<Object> hKeys(String key);

    Long hSize(String key);

    // ========== List操作 ==========
    Long lPush(String key, Object... values);

    Long rPush(String key, Object... values);

    <T> T lPop(String key, Class<T> clazz);

    <T> T rPop(String key, Class<T> clazz);

    <T> T lIndex(String key, long index, Class<T> clazz);

    <T> List<T> lRange(String key, long start, long end, Class<T> clazz);

    Long lSize(String key);

    Boolean lSet(String key, long index, Object value);

    Long lRemove(String key, long count, Object value);

    // ========== Set操作 ==========
    Long sAdd(String key, Object... values);

    <T> Set<T> sMembers(String key, Class<T> clazz);

    Boolean sIsMember(String key, Object value);

    Long sRemove(String key, Object... values);

    Long sSize(String key);

    <T> Set<T> sIntersect(String key, String otherKey, Class<T> clazz);

    <T> Set<T> sUnion(String key, String otherKey, Class<T> clazz);

    // ========= ZSet操作 ==========
    Boolean zAdd(String key, Object value, double score);

    Long zRemove(String key, Object... values);

    Double zIncrementScore(String key, Object value, double delta);

    Long zRank(String key, Object value);

    Set<Object> zRange(String key, long start, long end);

    Set<Object> zRangeByScore(String key, double min, double max);

    Long zSize(String key);

    Long zCount(String key, double min, double max);

    // ========== 分布式锁 ==========
    boolean tryLock(String lockKey, Duration expireTime);

    boolean tryLock(String lockKey, String lockValue, Duration expireTime);

    boolean releaseLock(String lockKey);

    boolean releaseLock(String lockKey, String lockValue);

    <T> T executeWithLock(String lockKey, Duration expireTime, Supplier<T> supplier);

    // ========== 脚本操作 ==========

    /**
     * 执行Lua脚本
     */
    <T> T execute(RedisScript<T> script, List<String> keys, Object... args);

    // ========== 缓存操作 ==========

    <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> loader);

    <T> T getOrLoad(String key, Class<T> clazz, Duration timeout, Supplier<T> loader);

    // ========== 工具方法 ==========

    Set<String> keys(String pattern);

    String buildKey(String... parts);

}
