package com.pot.zing.framework.starter.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.pot.zing.framework.starter.redis.properties.RedisProperties;
import com.pot.zing.framework.starter.redis.service.RedisService;
import com.pot.zing.framework.starter.redis.service.impl.RedisServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Auto-configuration for Redis template and service beans.
 */
@Slf4j
@AutoConfiguration(after = org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class)
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnProperty(prefix = "pot.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "potRedisTemplate")
    public RedisTemplate<String, Object> potRedisTemplate(RedisConnectionFactory connectionFactory,
            RedisProperties properties) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        Jackson2JsonRedisSerializer<Object> jsonSerializer = buildJsonSerializer(properties);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();

        log.info("Pot RedisTemplate initialized with serializer type: {}",
                properties.getSerializer().getType());

        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisService potRedisService(RedisTemplate<String, Object> potRedisTemplate,
            RedisProperties properties) {
        log.info("Pot RedisService initialized with key prefix: {}", properties.getKeyPrefix());
        return new RedisServiceImpl(potRedisTemplate, properties);
    }

    private Jackson2JsonRedisSerializer<Object> buildJsonSerializer(RedisProperties properties) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        if (properties.getSerializer().isEnableTyping()) {
            objectMapper.activateDefaultTyping(
                    LaissezFaireSubTypeValidator.instance,
                    ObjectMapper.DefaultTyping.NON_FINAL);
        }

        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }
}