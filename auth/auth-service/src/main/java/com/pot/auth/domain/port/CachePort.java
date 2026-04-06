package com.pot.auth.domain.port;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface CachePort {


        <T> void set(String key, T value, Duration ttl);

        <T> Optional<T> get(String key, Class<T> type);

        void delete(String key);

        void deleteBatch(Set<String> keys);

        boolean exists(String key);


        <T> void addToSet(String key, T value, Duration ttl);

        <T> void removeFromSet(String key, T value);

        <T> Set<T> getSet(String key, Class<T> type);

        <T> boolean isMemberOfSet(String key, T value);


        <T> void setHash(String key, String field, T value, Duration ttl);

        <T> Optional<T> getHash(String key, String field, Class<T> type);

        <T> Map<String, T> getAllHash(String key, Class<T> type);

        void deleteHash(String key, String field);


        long increment(String key, long delta, Duration ttl);

        long decrement(String key, long delta);


        <T> boolean setIfAbsent(String key, T value, Duration ttl);

        void expire(String key, Duration ttl);

        Optional<Duration> getTtl(String key);

        void persist(String key);
}

