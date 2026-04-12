package com.pot.auth.infrastructure.adapter.cache;

import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.authorization.constant.CacheKeyConstants;
import com.pot.zing.framework.starter.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cache port adapter backed by the shared Redis service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheAdapter implements CachePort {

    private final RedisService redisService;

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        String fullKey = buildAuthKey(key);
        Boolean success = redisService.set(fullKey, value, ttl);
        if (Boolean.FALSE.equals(success)) {
            log.error("Cache write failed — key={}", fullKey);
            throw new CacheException("Cache write failed: " + fullKey);
        }
        log.debug("Cache set — key={}, ttl={}", fullKey, ttl);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        String fullKey = buildAuthKey(key);
        T value = redisService.get(fullKey, type);
        if (value == null) {
            log.debug("Cache miss — key={}", fullKey);
            return Optional.empty();
        }
        log.debug("Cache hit — key={}", fullKey);
        return Optional.of(value);
    }

    @Override
    public void delete(String key) {
        String fullKey = buildAuthKey(key);
        redisService.delete(fullKey);
        log.debug("Cache evicted — key={}", fullKey);
    }

    @Override
    public void deleteBatch(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        Set<String> fullKeys = keys.stream()
                .map(this::buildAuthKey)
                .collect(Collectors.toSet());
        Long deleted = redisService.delete(fullKeys);
        log.debug("Bulk cache eviction — count={}", deleted);
    }

    @Override
    public boolean exists(String key) {
        String fullKey = buildAuthKey(key);
        Boolean exists = redisService.exists(fullKey);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public <T> void addToSet(String key, T value, Duration ttl) {
        String fullKey = buildAuthKey(key);
        redisService.sAdd(fullKey, value);
        redisService.expire(fullKey, ttl);
        log.debug("Set member added — key={}, value={}", fullKey, value);
    }

    @Override
    public <T> void removeFromSet(String key, T value) {
        String fullKey = buildAuthKey(key);
        redisService.sRemove(fullKey, value);
        log.debug("Set member removed — key={}, value={}", fullKey, value);
    }

    @Override
    public <T> Set<T> getSet(String key, Class<T> type) {
        String fullKey = buildAuthKey(key);
        Set<T> members = redisService.sMembers(fullKey, type);
        return members != null ? members : Collections.emptySet();
    }

    @Override
    public <T> boolean isMemberOfSet(String key, T value) {
        String fullKey = buildAuthKey(key);
        Boolean isMember = redisService.sIsMember(fullKey, value);
        return Boolean.TRUE.equals(isMember);
    }

    @Override
    public <T> void setHash(String key, String field, T value, Duration ttl) {
        String fullKey = buildAuthKey(key);
        redisService.hSet(fullKey, field, value);
        redisService.expire(fullKey, ttl);
        log.debug("Hash field set — key={}, field={}", fullKey, field);
    }

    @Override
    public <T> Optional<T> getHash(String key, String field, Class<T> type) {
        String fullKey = buildAuthKey(key);
        T value = redisService.hGet(fullKey, field, type);
        return Optional.ofNullable(value);
    }

    @Override
    public <T> Map<String, T> getAllHash(String key, Class<T> type) {
        String fullKey = buildAuthKey(key);
        Map<Object, Object> entries = redisService.hGetAll(fullKey);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new HashMap<>();
        entries.forEach((k, v) -> {
            String fieldKey = k.toString();
            @SuppressWarnings("unchecked")
            T value = (T) v;
            result.put(fieldKey, value);
        });
        return result;
    }

    @Override
    public void deleteHash(String key, String field) {
        String fullKey = buildAuthKey(key);
        redisService.hDelete(fullKey, field);
        log.debug("Hash field deleted — key={}, field={}", fullKey, field);
    }

    @Override
    public long increment(String key, long delta, Duration ttl) {
        String fullKey = buildAuthKey(key);
        Long result = redisService.increment(fullKey, delta);
        redisService.expire(fullKey, ttl);
        return result != null ? result : 0;
    }

    @Override
    public long decrement(String key, long delta) {
        String fullKey = buildAuthKey(key);
        Long result = redisService.increment(fullKey, -delta);
        return result != null ? result : 0;
    }

    @Override
    public <T> boolean setIfAbsent(String key, T value, Duration ttl) {
        String fullKey = buildAuthKey(key);
        Boolean success = redisService.setIfAbsent(fullKey, value, ttl);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void expire(String key, Duration ttl) {
        String fullKey = buildAuthKey(key);
        redisService.expire(fullKey, ttl);
        log.debug("TTL updated — key={}, ttl={}", fullKey, ttl);
    }

    @Override
    public Optional<Duration> getTtl(String key) {
        String fullKey = buildAuthKey(key);
        Duration ttl = redisService.getExpireDuration(fullKey);
        return Optional.ofNullable(ttl);
    }

    @Override
    public void persist(String key) {
        String fullKey = buildAuthKey(key);
        Boolean success = redisService.persist(fullKey);
        if (Boolean.TRUE.equals(success)) {
            log.debug("Key persisted — key={}", fullKey);
        } else {
            log.warn("Key persist failed — key={}", fullKey);
        }
    }

    /**
     * Builds the auth-scoped cache key passed to the shared Redis service.
     */
    private String buildAuthKey(String key) {
        return CacheKeyConstants.buildKey(key);
    }

    /**
     * Raised when a critical cache write fails.
     */
    public static class CacheException extends RuntimeException {
        public CacheException(String message) {
            super(message);
        }

        public CacheException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
