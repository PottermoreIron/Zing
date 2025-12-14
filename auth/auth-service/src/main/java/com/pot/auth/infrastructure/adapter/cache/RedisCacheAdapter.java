package com.pot.auth.infrastructure.adapter.cache;

import com.pot.auth.domain.port.CachePort;
import com.pot.auth.infrastructure.constant.CacheKeyConstants;
import com.pot.zing.framework.starter.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis缓存适配器（防腐层实现）
 *
 * <p>使用框架层的 RedisService 实现 CachePort 接口，提供工业级缓存能力
 * <p>优势：
 * <ul>
 *   <li>统一的 Redis 配置和管理</li>
 *   <li>自动的 key 前缀和命名空间隔离</li>
 *   <li>完善的错误处理和日志记录</li>
 *   <li>支持分布式锁、Lua脚本等高级特性</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheAdapter implements CachePort {

    private final RedisService redisService;

    // ========== 基本操作 ==========

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        String fullKey = buildAuthKey(key);
        Boolean success = redisService.set(fullKey, value, ttl);
        if (Boolean.FALSE.equals(success)) {
            log.error("缓存设置失败: key={}", fullKey);
            throw new CacheException("缓存设置失败: " + fullKey);
        }
        log.debug("缓存设置成功: key={}, ttl={}", fullKey, ttl);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        String fullKey = buildAuthKey(key);
        T value = redisService.get(fullKey, type);
        if (value == null) {
            log.debug("缓存未命中: key={}", fullKey);
            return Optional.empty();
        }
        log.debug("缓存命中: key={}", fullKey);
        return Optional.of(value);
    }

    @Override
    public void delete(String key) {
        String fullKey = buildAuthKey(key);
        redisService.delete(fullKey);
        log.debug("缓存删除: key={}", fullKey);
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
        log.debug("批量删除缓存: count={}", deleted);
    }

    @Override
    public boolean exists(String key) {
        String fullKey = buildAuthKey(key);
        Boolean exists = redisService.exists(fullKey);
        return Boolean.TRUE.equals(exists);
    }

    // ========== 集合操作 ==========

    @Override
    public <T> void addToSet(String key, T value, Duration ttl) {
        String fullKey = buildAuthKey(key);
        redisService.sAdd(fullKey, value);
        redisService.expire(fullKey, ttl);
        log.debug("集合添加元素: key={}, value={}", fullKey, value);
    }

    @Override
    public <T> void removeFromSet(String key, T value) {
        String fullKey = buildAuthKey(key);
        redisService.sRemove(fullKey, value);
        log.debug("集合删除元素: key={}, value={}", fullKey, value);
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

    // ========== Hash操作 ==========

    @Override
    public <T> void setHash(String key, String field, T value, Duration ttl) {
        String fullKey = buildAuthKey(key);
        redisService.hSet(fullKey, field, value);
        redisService.expire(fullKey, ttl);
        log.debug("Hash设置字段: key={}, field={}", fullKey, field);
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
        log.debug("Hash删除字段: key={}, field={}", fullKey, field);
    }

    // ========== 计数器操作 ==========

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

    // ========== 高级操作 ==========

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
        log.debug("设置过期时间: key={}, ttl={}", fullKey, ttl);
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
            log.debug("持久化成功: key={}", fullKey);
        } else {
            log.warn("持久化失败: key={}", fullKey);
        }
    }

    // ========== 工具方法 ==========

    /**
     * 构建带有 auth 前缀的完整 key
     * <p>最终格式：pot:auth:业务key（由 RedisService 自动添加 pot: 前缀）
     *
     * @param key 业务 key
     * @return 完整的 Redis key
     */
    private String buildAuthKey(String key) {
        return CacheKeyConstants.buildKey(key);
    }

    /**
     * 缓存异常
     * <p>用于关键操作失败时的异常抛出，体现领域层防腐策略
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

