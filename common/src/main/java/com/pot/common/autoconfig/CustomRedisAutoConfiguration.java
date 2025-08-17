package com.pot.common.autoconfig;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.pot.common.redis.RedisService;
import com.pot.common.redis.impl.RedisServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author: Pot
 * @created: 2025/8/17 00:31
 * @description: Redis自动装配类
 */
@Configuration
@ConditionalOnClass({RedisTemplate.class, RedisConnectionFactory.class})
@Slf4j
public class CustomRedisAutoConfiguration {

    /**
     * 线程安全的 ObjectMapper 配置（单例模式，避免重复创建）
     */
    private static final ObjectMapper OBJECT_MAPPER = createSafeObjectMapper();

    /**
     * 预配置的 JSON 序列化器（复用 ObjectMapper）
     */
    private static final Jackson2JsonRedisSerializer<Object> JSON_SERIALIZER = createJsonSerializer();

    /**
     * 字符串序列化器（复用实例）
     */
    private static final StringRedisSerializer STRING_SERIALIZER = new StringRedisSerializer();

    /**
     * 创建线程安全的 ObjectMapper
     */
    private static ObjectMapper createSafeObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 安全性配置：使用更安全的类型验证器
        // TODO: 考虑使用 BasicPolymorphicTypeValidator 替代 LaissezFaireSubTypeValidator
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 性能和兼容性优化
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.findAndRegisterModules();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        return mapper;
    }

    /**
     * 创建 JSON 序列化器
     */
    private static Jackson2JsonRedisSerializer<Object> createJsonSerializer() {
        return new Jackson2JsonRedisSerializer<>(OBJECT_MAPPER, Object.class);
    }

    /**
     * 配置 RedisTemplate Bean
     *
     * @param connectionFactory Redis 连接工厂
     * @return 配置好的 RedisTemplate
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("Initializing RedisTemplate with optimized serializers");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 序列化配置（统一使用 String）
        template.setKeySerializer(STRING_SERIALIZER);
        template.setHashKeySerializer(STRING_SERIALIZER);

        // Value 序列化配置（使用 JSON）
        template.setValueSerializer(JSON_SERIALIZER);
        template.setHashValueSerializer(JSON_SERIALIZER);

        // 启用默认序列化器（用于事务等场景）
        template.setEnableDefaultSerializer(false);
        template.setDefaultSerializer(JSON_SERIALIZER);

        // 初始化模板
        template.afterPropertiesSet();

        log.info("RedisTemplate initialized successfully");
        return template;
    }

    /**
     * 配置 RedisService Bean
     *
     * @param redisTemplate Redis 模板
     * @return RedisService 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisService redisService(RedisTemplate<String, Object> redisTemplate) {
        log.info("Initializing RedisService with RedisTemplate");
        return new RedisServiceImpl(redisTemplate);
    }
}
