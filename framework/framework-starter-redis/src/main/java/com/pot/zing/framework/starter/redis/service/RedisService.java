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
 * High-level Redis operations facade.
 */
public interface RedisService {

    /**
     * Sets a value.
     */
    Boolean set(String key, Object value);

    /**
     * Sets a value with a timeout.
     */
    Boolean set(String key, Object value, Duration timeout);

    /**
     * Sets a value with a timeout.
     */
    Boolean set(String key, Object value, long timeout, TimeUnit unit);

    /**
     * Sets a value when the key is absent.
     */
    Boolean setIfAbsent(String key, Object value);

    /**
     * Sets a value with a timeout when the key is absent.
     */
    Boolean setIfAbsent(String key, Object value, Duration timeout);

    /**
     * Sets a value with a timeout when the key is absent.
     */
    Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit);

    /**
     * Gets a value.
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * Deletes a key.
     */
    Boolean delete(String key);

    /**
     * Deletes multiple keys.
     */
    Long delete(Collection<String> keys);

    /**
     * Checks whether a key exists.
     */
    Boolean exists(String key);

    /**
     * Sets the expiration time.
     */
    Boolean expire(String key, Duration timeout);

    /**
     * Sets the expiration time.
     */
    Boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * Returns the remaining TTL in seconds.
     */
    Long getExpire(String key);

    /**
     * Returns the remaining TTL as a duration.
     */
    Duration getExpireDuration(String key);

    /**
     * Removes the expiration time from a key.
     */
    Boolean persist(String key);

    /**
     * Returns the Redis type for the key.
     */
    String type(String key);

    /**
     * Increments a numeric key by the supplied delta.
     */
    Long increment(String key, long delta);

    /**
     * Increments a numeric key by one.
     */
    Long increment(String key);

    /**
     * Increments a numeric key by a floating-point delta.
     */
    Double increment(String key, double delta);

    /**
     * Sets a hash field.
     */
    Boolean hSet(String key, String field, Object value);

    Boolean hSetAll(String key, Map<String, Object> map);

    Boolean hSetIfAbsent(String key, String field, Object value);

    /**
     * Gets a hash field.
     */

    <T> T hGet(String key, String field, Class<T> clazz);

    Map<Object, Object> hGetAll(String key);

    Long hDelete(String key, String... fields);

    Boolean hExists(String key, String field);

    Long hIncrement(String key, String field, long delta);

    Set<Object> hKeys(String key);

    Long hSize(String key);

    Long lPush(String key, Object... values);

    Long rPush(String key, Object... values);

    <T> T lPop(String key, Class<T> clazz);

    <T> T rPop(String key, Class<T> clazz);

    <T> T lIndex(String key, long index, Class<T> clazz);

    <T> List<T> lRange(String key, long start, long end, Class<T> clazz);

    Long lSize(String key);

    Boolean lSet(String key, long index, Object value);

    Long lRemove(String key, long count, Object value);

    Long sAdd(String key, Object... values);

    <T> Set<T> sMembers(String key, Class<T> clazz);

    Boolean sIsMember(String key, Object value);

    Long sRemove(String key, Object... values);

    Long sSize(String key);

    <T> Set<T> sIntersect(String key, String otherKey, Class<T> clazz);

    <T> Set<T> sUnion(String key, String otherKey, Class<T> clazz);

    Boolean zAdd(String key, Object value, double score);

    Long zRemove(String key, Object... values);

    Double zIncrementScore(String key, Object value, double delta);

    Long zRank(String key, Object value);

    Set<Object> zRange(String key, long start, long end);

    Set<Object> zRangeByScore(String key, double min, double max);

    Long zSize(String key);

    Long zCount(String key, double min, double max);

    boolean tryLock(String lockKey, Duration expireTime);

    boolean tryLock(String lockKey, String lockValue, Duration expireTime);

    boolean releaseLock(String lockKey);

    boolean releaseLock(String lockKey, String lockValue);

    <T> T executeWithLock(String lockKey, Duration expireTime, Supplier<T> supplier);

    /**
     * Executes a Lua script.
     */
    <T> T execute(RedisScript<T> script, List<String> keys, Object... args);

    <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> loader);

    <T> T getOrLoad(String key, Class<T> clazz, Duration timeout, Supplier<T> loader);

    Set<String> keys(String pattern);

    String buildKey(String... parts);

}
