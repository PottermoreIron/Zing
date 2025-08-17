package com.pot.common.redis;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/8/17 17:09
 * @description: Redis接口类
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
    Object get(String key);

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
    Object hGet(String key, String field);

    <T> T hGet(String key, String field, Class<T> clazz);

    Map<String, Object> hGetAll(String key);

    Boolean hDel(String key, String... fields);

    Boolean hExists(String key, String field);

    Long hIncrement(String key, String field, long delta);

    // ========== List操作 ==========
    Long lPush(String key, Object... values);

    Long rPush(String key, Object... values);

    <T> T lPop(String key, Class<T> clazz);

    <T> T rPop(String key, Class<T> clazz);

    <T> T lIndex(String key, long index, Class<T> clazz);

    <T> List<T> lRange(String key, long start, long end, Class<T> clazz);

    Long lSize(String key);

    // ========== Set操作 ==========
    Long sAdd(String key, Object... values);

    <T> Set<T> sMembers(String key, Class<T> clazz);

    Boolean sIsMember(String key, Object value);

    Long sRem(String key, Object... values);

    // ========= ZSet操作 ==========
    Boolean zAdd(String key, Object value, double score);

    Long zSize(String key);

    Set<Object> zRange(String key, long start, long end);

    Set<Object> zRangeByScore(String key, double min, double max);

    // ========== 分布式锁 ==========
    boolean tryLock(String lockKey, String lockValue, Duration expireTime);

    boolean releaseLock(String lockKey, String lockValue);
}
