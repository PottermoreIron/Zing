package com.pot.user.service.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author: Pot
 * @created: 2025/3/16 22:36
 * @description: redis配置类
 */
@Configuration
public class RedisTemplateConfiguration {
    /**
     * 线程安全的 ObjectMapper 配置（避免重复创建）
     */
    private static final ObjectMapper SAFE_OBJECT_MAPPER = createObjectMapper();
    /**
     * 预配置的 JSON 序列化器
     */
    private static final Jackson2JsonRedisSerializer<Object> JSON_SERIALIZER =
            new Jackson2JsonRedisSerializer<>(SAFE_OBJECT_MAPPER, Object.class);

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // todo 替换LaissezFaireSubTypeValidator
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        // 性能优化配置
        mapper.findAndRegisterModules();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // Key 序列化统一处理
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        // Value 序列化配置
        redisTemplate.setValueSerializer(JSON_SERIALIZER);
        redisTemplate.setHashValueSerializer(JSON_SERIALIZER);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
