package com.pot.zing.framework.starter.redis.service.impl;

import com.pot.zing.framework.starter.redis.properties.RedisProperties;
import com.pot.zing.framework.starter.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisProperties properties;

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end";

    private static final ThreadLocal<String> LOCK_VALUE_HOLDER = new ThreadLocal<>();

    // ========== String 操作 ==========

    @Override
    public Boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(buildKey(key), value);
            return true;
        } catch (Exception e) {
            log.error("Redis set failed: key={}", key, e);
            return false;
        }
    }

    @Override
    public Boolean set(String key, Object value, Duration timeout) {
        try {
            redisTemplate.opsForValue().set(buildKey(key), value, timeout);
            return true;
        } catch (Exception e) {
            log.error("Redis set with timeout failed: key={}", key, e);
            return false;
        }
    }

    @Override
    public Boolean set(String key, Object value, long timeout, TimeUnit unit) {
        return set(key, value, Duration.ofMillis(unit.toMillis(timeout)));
    }

    @Override
    public Boolean setIfAbsent(String key, Object value) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(buildKey(key), value);
        } catch (Exception e) {
            log.error("Redis setIfAbsent failed: key={}", key, e);
            return false;
        }
    }

    @Override
    public Boolean setIfAbsent(String key, Object value, Duration timeout) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(buildKey(key), value, timeout);
        } catch (Exception e) {
            log.error("Redis setIfAbsent with timeout failed: key={}", key, e);
            return false;
        }
    }

    @Override
    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("Redis setIfAbsent with time unit operation failed, key: {}", key, e);
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(buildKey(key));
            return value == null ? null : (T) value;
        } catch (Exception e) {
            log.error("Redis get failed: key={}", key, e);
            return null;
        }
    }

    @Override
    public Boolean delete(String key) {
        try {
            return redisTemplate.delete(buildKey(key));
        } catch (Exception e) {
            log.error("Redis delete failed: key={}", key, e);
            return false;
        }
    }

    @Override
    public Long delete(Collection<String> keys) {
        try {
            Set<String> fullKeys = keys.stream()
                    .map(this::buildKey)
                    .collect(Collectors.toSet());
            return redisTemplate.delete(fullKeys);
        } catch (Exception e) {
            log.error("Redis batch delete failed: keys={}", keys, e);
            return 0L;
        }
    }

    @Override
    public Boolean exists(String key) {
        try {
            return redisTemplate.hasKey(buildKey(key));
        } catch (Exception e) {
            log.error("Redis exists failed: key={}", key, e);
            return false;
        }
    }

    @Override
    public Boolean expire(String key, Duration timeout) {
        try {
            return redisTemplate.expire(buildKey(key), timeout);
        } catch (Exception e) {
            log.error("Redis expire failed: key={}", key, e);
            return false;
        }
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis expire with time unit operation failed, key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取剩余过期时间
     *
     * @param key 键名
     * @return -2L表示不存在，-1L表示没有设置过期时间
     */
    @Override
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(buildKey(key), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis getExpire failed: key={}", key, e);
            return -2L;
        }
    }

    @Override
    public String type(String key) {
        try {
            return redisTemplate.type(key).code();
        } catch (Exception e) {
            log.error("Redis type operation failed, key: {}", key, e);
            return null;
        }
    }

    @Override
    public Long increment(String key) {
        return increment(key, 1L);
    }

    @Override
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(buildKey(key), delta);
        } catch (Exception e) {
            log.error("Redis increment failed: key={}, delta={}", key, delta, e);
            return null;
        }
    }

    @Override
    public Double increment(String key, double delta) {
        try {
            return redisTemplate.opsForValue().increment(buildKey(key), delta);
        } catch (Exception e) {
            log.error("Redis increment failed: key={}, delta={}", key, delta, e);
            return null;
        }
    }

    // ========== Hash 操作 ==========

    @Override
    public Boolean hSet(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(buildKey(key), field, value);
            return true;
        } catch (Exception e) {
            log.error("Redis hSet failed: key={}, field={}", key, field, e);
            return false;
        }
    }

    @Override
    public Boolean hSetAll(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(buildKey(key), map);
            return true;
        } catch (Exception e) {
            log.error("Redis hSetAll failed: key={}", key, e);
            return false;
        }
    }

    @Override
    public Boolean hSetIfAbsent(String key, String field, Object value) {
        try {
            return redisTemplate.opsForHash().putIfAbsent(key, field, value);
        } catch (Exception e) {
            log.error("Redis hSetIfAbsent operation failed, key: {}, field: {}", key, field, e);
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String field, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForHash().get(buildKey(key), field);
            return value == null ? null : (T) value;
        } catch (Exception e) {
            log.error("Redis hGet failed: key={}, field={}", key, field, e);
            return null;
        }
    }

    @Override
    public Map<Object, Object> hGetAll(String key) {
        try {
            return redisTemplate.opsForHash().entries(buildKey(key));
        } catch (Exception e) {
            log.error("Redis hGetAll failed: key={}", key, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Long hDelete(String key, String... fields) {
        try {
            return redisTemplate.opsForHash().delete(buildKey(key), (Object[]) fields);
        } catch (Exception e) {
            log.error("Redis hDelete failed: key={}, fields={}", key, Arrays.toString(fields), e);
            return 0L;
        }
    }

    @Override
    public Boolean hExists(String key, String field) {
        try {
            return redisTemplate.opsForHash().hasKey(buildKey(key), field);
        } catch (Exception e) {
            log.error("Redis hExists failed: key={}, field={}", key, field, e);
            return false;
        }
    }

    @Override
    public Long hIncrement(String key, String field, long delta) {
        try {
            return redisTemplate.opsForHash().increment(buildKey(key), field, delta);
        } catch (Exception e) {
            log.error("Redis hIncrement failed: key={}, field={}, delta={}", key, field, delta, e);
            return null;
        }
    }

    @Override
    public Set<Object> hKeys(String key) {
        try {
            return redisTemplate.opsForHash().keys(buildKey(key));
        } catch (Exception e) {
            log.error("Redis hKeys failed: key={}", key, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取Hash键的大小
     *
     * @param key 键名
     * @return -1L表示失败
     */
    @Override
    public Long hSize(String key) {
        try {
            return redisTemplate.opsForHash().size(buildKey(key));
        } catch (Exception e) {
            log.error("Redis hSize failed: key={}", key, e);
            return -1L;
        }
    }

    // ========== List 操作 ==========

    @Override
    public Long lPush(String key, Object... values) {
        try {
            return redisTemplate.opsForList().leftPushAll(buildKey(key), values);
        } catch (Exception e) {
            log.error("Redis lPush failed: key={}", key, e);
            return 0L;
        }
    }

    @Override
    public Long rPush(String key, Object... values) {
        try {
            return redisTemplate.opsForList().rightPushAll(buildKey(key), values);
        } catch (Exception e) {
            log.error("Redis rPush failed: key={}", key, e);
            return 0L;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T lPop(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForList().leftPop(buildKey(key));
            return value == null ? null : (T) value;
        } catch (Exception e) {
            log.error("Redis lPop failed: key={}", key, e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T rPop(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForList().rightPop(buildKey(key));
            return value == null ? null : (T) value;
        } catch (Exception e) {
            log.error("Redis rPop failed: key={}", key, e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T lIndex(String key, long index, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForList().index(key, index);
            return value == null ? null : (T) value;
        } catch (Exception e) {
            log.error("Redis lIndex operation failed, key: {}, index: {}", key, index, e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> lRange(String key, long start, long end, Class<T> clazz) {
        try {
            List<Object> values = redisTemplate.opsForList().range(buildKey(key), start, end);
            return values == null ? Collections.emptyList() : (List<T>) values;
        } catch (Exception e) {
            log.error("Redis lRange failed: key={}", key, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取List键的大小
     *
     * @param key 键名
     * @return -1L表示失败
     */
    @Override
    public Long lSize(String key) {
        try {
            return redisTemplate.opsForList().size(buildKey(key));
        } catch (Exception e) {
            log.error("Redis lSize failed: key={}", key, e);
            return -1L;
        }
    }

    @Override
    public Boolean lSet(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(buildKey(key), index, value);
            return true;
        } catch (Exception e) {
            log.error("Redis lSet failed: key={}, index={}", key, index, e);
            return false;
        }
    }

    @Override
    public Long lRemove(String key, long count, Object value) {
        try {
            return redisTemplate.opsForList().remove(buildKey(key), count, value);
        } catch (Exception e) {
            log.error("Redis lRemove failed: key={}", key, e);
            return 0L;
        }
    }

    // ========== Set 操作 ==========

    @Override
    public Long sAdd(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(buildKey(key), values);
        } catch (Exception e) {
            log.error("Redis sAdd failed: key={}", key, e);
            return 0L;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> sMembers(String key, Class<T> clazz) {
        try {
            Set<Object> values = redisTemplate.opsForSet().members(buildKey(key));
            return values == null ? Collections.emptySet() : (Set<T>) values;
        } catch (Exception e) {
            log.error("Redis sMembers failed: key={}", key, e);
            return Collections.emptySet();
        }
    }

    @Override
    public Boolean sIsMember(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(buildKey(key), value);
        } catch (Exception e) {
            log.error("Redis sIsMember failed: key={}", key, e);
            return false;
        }
    }

    @Override
    public Long sRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(buildKey(key), values);
        } catch (Exception e) {
            log.error("Redis sRemove failed: key={}", key, e);
            return 0L;
        }
    }

    /**
     * 获取Set键的大小
     *
     * @param key 键名
     * @return -1L表示失败
     */
    @Override
    public Long sSize(String key) {
        try {
            return redisTemplate.opsForSet().size(buildKey(key));
        } catch (Exception e) {
            log.error("Redis sSize failed: key={}", key, e);
            return -1L;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> sIntersect(String key, String otherKey, Class<T> clazz) {
        try {
            Set<Object> values = redisTemplate.opsForSet().intersect(buildKey(key), buildKey(otherKey));
            return values == null ? Collections.emptySet() : (Set<T>) values;
        } catch (Exception e) {
            log.error("Redis sIntersect failed: key={}, otherKey={}", key, otherKey, e);
            return Collections.emptySet();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> sUnion(String key, String otherKey, Class<T> clazz) {
        try {
            Set<Object> values = redisTemplate.opsForSet().union(buildKey(key), buildKey(otherKey));
            return values == null ? Collections.emptySet() : (Set<T>) values;
        } catch (Exception e) {
            log.error("Redis sUnion failed: key={}, otherKey={}", key, otherKey, e);
            return Collections.emptySet();
        }
    }

    // ========== ZSet 操作 ==========

    @Override
    public Boolean zAdd(String key, Object value, double score) {
        try {
            return redisTemplate.opsForZSet().add(buildKey(key), value, score);
        } catch (Exception e) {
            log.error("Redis zAdd failed: key={}, score={}", key, score, e);
            return false;
        }
    }

    @Override
    public Long zRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForZSet().remove(buildKey(key), values);
        } catch (Exception e) {
            log.error("Redis zRemove failed: key={}", key, e);
            return 0L;
        }
    }

    @Override
    public Double zIncrementScore(String key, Object value, double delta) {
        try {
            return redisTemplate.opsForZSet().incrementScore(buildKey(key), value, delta);
        } catch (Exception e) {
            log.error("Redis zIncrementScore failed: key={}, delta={}", key, delta, e);
            return null;
        }
    }

    @Override
    public Long zRank(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().rank(buildKey(key), value);
        } catch (Exception e) {
            log.error("Redis zRank failed: key={}", key, e);
            return null;
        }
    }

    @Override
    public Set<Object> zRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().range(buildKey(key), start, end);
        } catch (Exception e) {
            log.error("Redis zRange failed: key={}", key, e);
            return Collections.emptySet();
        }
    }

    @Override
    public Set<Object> zRangeByScore(String key, double min, double max) {
        try {
            return redisTemplate.opsForZSet().rangeByScore(buildKey(key), min, max);
        } catch (Exception e) {
            log.error("Redis zRangeByScore failed: key={}", key, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取ZSet键的大小
     *
     * @param key 键名
     * @return -1L表示失败
     */
    @Override
    public Long zSize(String key) {
        try {
            return redisTemplate.opsForZSet().size(buildKey(key));
        } catch (Exception e) {
            log.error("Redis zSize failed: key={}", key, e);
            return -1L;
        }
    }

    @Override
    public Long zCount(String key, double min, double max) {
        try {
            return redisTemplate.opsForZSet().count(buildKey(key), min, max);
        } catch (Exception e) {
            log.error("Redis zCount failed: key={}", key, e);
            return 0L;
        }
    }

    // ========== 分布式锁 ==========

    @Override
    public boolean tryLock(String lockKey, Duration expireTime) {
        String lockValue = UUID.randomUUID().toString();
        boolean locked = tryLock(lockKey, lockValue, expireTime);
        if (locked) {
            LOCK_VALUE_HOLDER.set(lockValue);
        }
        return locked;
    }

    @Override
    public boolean tryLock(String lockKey, String lockValue, Duration expireTime) {
        try {
            String fullKey = buildKey(properties.getLock().getPrefix(), lockKey);
            Boolean result = redisTemplate.opsForValue().setIfAbsent(fullKey, lockValue, expireTime);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis tryLock failed: lockKey={}", lockKey, e);
            return false;
        }
    }

    @Override
    public boolean releaseLock(String lockKey) {
        String lockValue = LOCK_VALUE_HOLDER.get();
        try {
            return releaseLock(lockKey, lockValue);
        } finally {
            LOCK_VALUE_HOLDER.remove();
        }
    }

    @Override
    public boolean releaseLock(String lockKey, String lockValue) {
        try {
            String fullKey = buildKey(properties.getLock().getPrefix(), lockKey);
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(fullKey), lockValue);
            return Long.valueOf(1).equals(result);
        } catch (Exception e) {
            log.error("Redis releaseLock failed: lockKey={}", lockKey, e);
            return false;
        }
    }

    @Override
    public <T> T executeWithLock(String lockKey, Duration expireTime, Supplier<T> supplier) {
        boolean locked = tryLock(lockKey, expireTime);
        if (!locked) {
            throw new IllegalStateException("Failed to acquire lock: " + lockKey);
        }
        try {
            return supplier.get();
        } finally {
            releaseLock(lockKey);
        }
    }

    @Override
    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        try {
            // 构建完整的key
            List<String> fullKeys = keys.stream()
                    .map(this::buildKey)
                    .collect(Collectors.toList());

            return redisTemplate.execute(script, fullKeys, args);
        } catch (Exception e) {
            log.error("Redis execute script failed: keys={}", keys, e);
            return null;
        }
    }

    // ========== 缓存操作 ==========

    @Override
    public <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> loader) {
        return getOrLoad(key, clazz, Duration.ofSeconds(properties.getDefaultExpireTime()), loader);
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> clazz, Duration timeout, Supplier<T> loader) {
        T value = get(key, clazz);
        if (value != null) {
            return value;
        }

        value = loader.get();
        if (value != null) {
            set(key, value, timeout);
        }
        return value;
    }

    // ========== 工具方法 ==========

    @Override
    public Set<String> keys(String pattern) {
        try {
            String fullPattern = buildKey(pattern);
            Set<String> keys = redisTemplate.keys(fullPattern);
            if (keys == null) {
                return Collections.emptySet();
            }
            String prefix = buildKey("");
            return keys.stream()
                    .map(key -> key.substring(prefix.length()))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Redis keys failed: pattern={}", pattern, e);
            return Collections.emptySet();
        }
    }

    @Override
    public String buildKey(String... parts) {
        if (parts == null || parts.length == 0) {
            return properties.getKeyPrefix();
        }
        return properties.getKeyPrefix() + String.join(properties.getKeySeparator(), parts);
    }
}
