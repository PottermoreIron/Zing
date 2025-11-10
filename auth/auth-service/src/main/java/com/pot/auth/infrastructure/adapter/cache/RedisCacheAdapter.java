package com.pot.auth.infrastructure.adapter.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.auth.domain.port.CachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis缓��适配器（防腐层实现）
 *
 * <p>将CachePort接口适配到Spring Data Redis
 * <p>领域层通过CachePort接口访问，完全不知道底层使用了Redis
 *
 * @author pot
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "auth.cache.type", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisCacheAdapter implements CachePort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Redis缓存设置成功: key={}, ttl={}ms", key, ttl.toMillis());
        } catch (Exception e) {
            log.error("Redis缓存设置失败: key={}", key, e);
            throw new CacheException("缓存设置失败", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Redis缓存未命中: key={}", key);
                return Optional.empty();
            }

            // 如果类型匹配，直接返回
            if (type.isInstance(value)) {
                log.debug("Redis缓存命中: key={}", key);
                return Optional.of((T) value);
            }

            // 尝试转换
            T converted = objectMapper.convertValue(value, type);
            log.debug("Redis缓存命中（转换）: key={}", key);
            return Optional.of(converted);
        } catch (Exception e) {
            log.error("Redis缓存读取失败: key={}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Redis缓存删除: key={}", key);
        } catch (Exception e) {
            log.error("Redis缓存删除失败: key={}", key, e);
        }
    }

    @Override
    public void deleteBatch(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        try {
            redisTemplate.delete(keys);
            log.debug("Redis批量删除缓存: count={}", keys.size());
        } catch (Exception e) {
            log.error("Redis批量删除失败: keys={}", keys, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis exists检查失败: key={}", key, e);
            return false;
        }
    }

    @Override
    public <T> void addToSet(String key, T value, Duration ttl) {
        try {
            redisTemplate.opsForSet().add(key, value);
            redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Redis集合添加: key={}, value={}", key, value);
        } catch (Exception e) {
            log.error("Redis集合添加失败: key={}", key, e);
        }
    }

    @Override
    public <T> void removeFromSet(String key, T value) {
        try {
            redisTemplate.opsForSet().remove(key, value);
            log.debug("Redis集合删除: key={}, value={}", key, value);
        } catch (Exception e) {
            log.error("Redis集合删除失败: key={}", key, e);
        }
    }

    @Override
    public <T> Set<T> getSet(String key, Class<T> type) {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(key);
            if (members == null || members.isEmpty()) {
                return Collections.emptySet();
            }

            return members.stream()
                    .map(obj -> objectMapper.convertValue(obj, type))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Redis集合读取失败: key={}", key, e);
            return Collections.emptySet();
        }
    }

    @Override
    public <T> boolean isMemberOfSet(String key, T value) {
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(key, value);
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("Redis集合成员检查失败: key={}", key, e);
            return false;
        }
    }

    @Override
    public <T> void setHash(String key, String field, T value, Duration ttl) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Redis Hash设置: key={}, field={}", key, field);
        } catch (Exception e) {
            log.error("Redis Hash设置失败: key={}, field={}", key, field, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getHash(String key, String field, Class<T> type) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            if (value == null) {
                return Optional.empty();
            }

            if (type.isInstance(value)) {
                return Optional.of((T) value);
            }

            T converted = objectMapper.convertValue(value, type);
            return Optional.of(converted);
        } catch (Exception e) {
            log.error("Redis Hash读取失败: key={}, field={}", key, field, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> Map<String, T> getAllHash(String key, Class<T> type) {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<String, T> result = new HashMap<>();
            entries.forEach((k, v) -> {
                String fieldKey = k.toString();
                T value = objectMapper.convertValue(v, type);
                result.put(fieldKey, value);
            });
            return result;
        } catch (Exception e) {
            log.error("Redis Hash全部读取失败: key={}", key, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public void deleteHash(String key, String field) {
        try {
            redisTemplate.opsForHash().delete(key, field);
            log.debug("Redis Hash删除: key={}, field={}", key, field);
        } catch (Exception e) {
            log.error("Redis Hash删除失败: key={}, field={}", key, field, e);
        }
    }

    @Override
    public long increment(String key, long delta, Duration ttl) {
        try {
            Long result = redisTemplate.opsForValue().increment(key, delta);
            redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Redis递增失败: key={}", key, e);
            return 0;
        }
    }

    @Override
    public long decrement(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().decrement(key, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Redis递减失败: key={}", key, e);
            return 0;
        }
    }

    @Override
    public <T> boolean setIfAbsent(String key, T value, Duration ttl) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(
                    key, value, ttl.toMillis(), TimeUnit.MILLISECONDS
            );
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis setIfAbsent失败: key={}", key, e);
            return false;
        }
    }

    @Override
    public void expire(String key, Duration ttl) {
        try {
            redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Redis设置过期时间: key={}, ttl={}ms", key, ttl.toMillis());
        } catch (Exception e) {
            log.error("Redis设置过期时间失败: key={}", key, e);
        }
    }

    @Override
    public Optional<Duration> getTtl(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            if (ttl < 0) {
                return Optional.empty();
            }
            return Optional.of(Duration.ofMillis(ttl));
        } catch (Exception e) {
            log.error("Redis获取TTL失败: key={}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void persist(String key) {
        try {
            redisTemplate.persist(key);
            log.debug("Redis持久化: key={}", key);
        } catch (Exception e) {
            log.error("Redis持久化失败: key={}", key, e);
        }
    }

    /**
     * 缓存异常
     */
    public static class CacheException extends RuntimeException {
        public CacheException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

