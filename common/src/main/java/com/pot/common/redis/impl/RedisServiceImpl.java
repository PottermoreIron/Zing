package com.pot.common.redis.impl;

import com.pot.common.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/8/17 17:11
 * @description: Redis接口实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 分布式锁释放脚本
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else " +
                    "return 0 " +
                    "end";

    @Override
    public Boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis set operation failed, key: {}", key, e);
            return false;
        }
    }

    @Override
    public Boolean set(String key, Object value, Duration timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout);
            return true;
        } catch (Exception e) {
            log.error("Redis set with timeout operation failed, key: {}", key, e);
            return false;
        }
    }

    @Override
    public Boolean set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            return true;
        } catch (Exception e) {
            log.error("Redis set with time unit operation failed, key: {}", key, e);
            return false;
        }
    }

    @Override
    public Boolean setIfAbsent(String key, Object value) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value);
        } catch (Exception e) {
            log.error("Redis setIfAbsent operation failed, key: {}", key, e);
            return false;
        }
    }

    @Override
    public Boolean setIfAbsent(String key, Object value, Duration timeout) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value, timeout);
        } catch (Exception e) {
            log.error("Redis setIfAbsent with timeout operation failed, key: {}", key, e);
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
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis get operation failed, key: {}", key, e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value == null ? null : (T) value;
        } catch (Exception e) {
            log.error("Redis get operation failed, key: {}", key, e);
            return null;
        }
    }

    @Override
    public Boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis delete operation failed, key: {}", key, e);
            return false;
        }
    }

    @Override
    public Long delete(Collection<String> keys) {
        try {
            return redisTemplate.delete(keys);
        } catch (Exception e) {
            log.error("Redis batch delete operation failed, keys: {}", keys, e);
            return 0L;
        }
    }

    @Override
    public Boolean exists(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis exists operation failed, key: {}", key, e);
            return false;
        }
    }

    @Override
    public Boolean expire(String key, Duration timeout) {
        try {
            return redisTemplate.expire(key, timeout);
        } catch (Exception e) {
            log.error("Redis expire operation failed, key: {}", key, e);
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
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("获取过期时间失败: key={}", key, e);
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
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Redis increment operation failed, key: {}, delta: {}", key, delta, e);
            return null;
        }
    }

    @Override
    public Long increment(String key) {
        return increment(key, 1L);
    }

    @Override
    public Double increment(String key, double delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Redis increment operation failed, key: {}, delta: {}", key, delta, e);
            return null;
        }
    }

    // Hash操作实现
    @Override
    public Boolean hSet(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            return true;
        } catch (Exception e) {
            log.error("Redis hSet operation failed, key: {}, field: {}", key, field, e);
            return false;
        }
    }

    @Override
    public Boolean hSetAll(String key, Map<String, Object> map) {
        try {
            if (CollectionUtils.isEmpty(map)) {
                return true;
            }
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.error("Redis hSetAll operation failed, key: {}, map: {}", key, map, e);
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
    public Object hGet(String key, String field) {
        try {
            return redisTemplate.opsForHash().get(key, field);
        } catch (Exception e) {
            log.error("Redis hGet operation failed, key: {}, field: {}", key, field, e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String field, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            return value == null ? null : (T) value;
        } catch (Exception e) {
            log.error("Redis hGet operation failed, key: {}, field: {}", key, field, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> hGetAll(String key) {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Object> result = new HashMap<>(entries.size());
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        } catch (Exception e) {
            log.error("Redis hGetAll operation failed, key: {}", key, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Boolean hDel(String key, String... fields) {
        try {
            Long deleted = redisTemplate.opsForHash().delete(key, (Object[]) fields);
            return deleted > 0;
        } catch (Exception e) {
            log.error("Redis hDel operation failed, key: {}, fields: {}", key, Arrays.toString(fields), e);
            return false;
        }
    }

    @Override
    public Boolean hExists(String key, String field) {
        try {
            return redisTemplate.opsForHash().hasKey(key, field);
        } catch (Exception e) {
            log.error("Redis hExists operation failed, key: {}, field: {}", key, field, e);
            return false;
        }
    }

    @Override
    public Long hIncrement(String key, String field, long delta) {
        try {
            return redisTemplate.opsForHash().increment(key, field, delta);
        } catch (Exception e) {
            log.error("Redis hIncrement operation failed, key: {}, field: {}, delta: {}", key, field, delta, e);
            return null;
        }
    }

    // List操作实现
    @Override
    public Long lPush(String key, Object... values) {
        try {
            return redisTemplate.opsForList().leftPushAll(key, values);
        } catch (Exception e) {
            log.error("Redis lPush operation failed, key: {}", key, e);
            return 0L;
        }
    }

    @Override
    public Long rPush(String key, Object... values) {
        try {
            return redisTemplate.opsForList().rightPushAll(key, values);
        } catch (Exception e) {
            log.error("Redis rPush operation failed, key: {}", key, e);
            return 0L;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T lPop(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForList().leftPop(key);
            return value == null ? null : (T) value;
        } catch (Exception e) {
            log.error("Redis lPop operation failed, key: {}", key, e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T rPop(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForList().rightPop(key);
            return value == null ? null : (T) value;
        } catch (Exception e) {
            log.error("Redis rPop operation failed, key: {}", key, e);
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
            List<Object> values = redisTemplate.opsForList().range(key, start, end);
            if (values == null) return Collections.emptyList();
            return (List<T>) values;
        } catch (Exception e) {
            log.error("Redis lRange operation failed, key: {}", key, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long lSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.error("Redis lSize operation failed, key: {}", key, e);
            return 0L;
        }
    }

    // Set操作实现
    @Override
    public Long sAdd(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.error("Redis sAdd operation failed, key: {}", key, e);
            return 0L;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> sMembers(String key, Class<T> clazz) {
        try {
            Set<Object> values = redisTemplate.opsForSet().members(key);
            if (values == null) return Collections.emptySet();
            return (Set<T>) values;
        } catch (Exception e) {
            log.error("Redis sMembers operation failed, key: {}", key, e);
            return Collections.emptySet();
        }
    }

    @Override
    public Boolean sIsMember(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            log.error("Redis sIsMember operation failed, key: {}", key, e);
            return false;
        }
    }

    @Override
    public Long sRem(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            log.error("Redis srem operation failed, key: {}", key, e);
            return 0L;
        }
    }

    @Override
    public Boolean zAdd(String key, Object value, double score) {
        try {
            return redisTemplate.opsForZSet().add(key, value, score);
        } catch (Exception e) {
            log.error("Redis zAdd operation failed, key: {}, value: {}, score: {}", key, value, score, e);
            return false;
        }
    }

    @Override
    public Long zSize(String key) {
        try {
            return redisTemplate.opsForZSet().size(key);
        } catch (Exception e) {
            log.error("Redis zSize operation failed, key: {}", key, e);
            return 0L;
        }
    }

    @Override
    public Set<Object> zRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().range(key, start, end);
        } catch (Exception e) {
            log.error("Redis zRange operation failed, key: {}, start: {}, end: {}", key, start, end, e);
            return Collections.emptySet();
        }
    }

    @Override
    public Set<Object> zRangeByScore(String key, double min, double max) {
        try {
            return redisTemplate.opsForZSet().rangeByScore(key, min, max);
        } catch (Exception e) {
            log.error("Redis zRangeByScore operation failed, key: {}, min: {}, max: {}", key, min, max, e);
            return Collections.emptySet();
        }
    }

    // 分布式锁实现
    @Override
    public boolean tryLock(String lockKey, String lockValue, Duration expireTime) {
        try {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expireTime);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis tryLock operation failed, lockKey: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public boolean releaseLock(String lockKey, String lockValue) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);
            return Long.valueOf(1).equals(result);
        } catch (Exception e) {
            log.error("Redis releaseLock operation failed, lockKey: {}", lockKey, e);
            return false;
        }
    }
}
