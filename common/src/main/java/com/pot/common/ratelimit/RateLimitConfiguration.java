package com.pot.common.ratelimit;

import com.pot.common.ratelimit.impl.GuavaRateLimitManager;
import com.pot.common.ratelimit.impl.IpBasedRateLimitKeyProvider;
import com.pot.common.ratelimit.impl.RedisRateLimitManager;
import com.pot.common.ratelimit.impl.UserBasedRateLimitKeyProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author: Pot
 * @created: 2025/3/30 21:47
 * @description: 限流自动配置入口类
 */
@Configuration
@ConditionalOnProperty(prefix = "ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(RateLimitAutoConfiguration.class)
@Slf4j
public class RateLimitConfiguration {
    /**
     * 创建默认的限流管理器
     * 如果需要自定义实现，可以在应用中提供一个RateLimitManager的Bean
     *
     * @return RateLimitManager实例
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitManager.class)
    @ConditionalOnProperty(prefix = "ratelimit", name = "provider", havingValue = "guava", matchIfMissing = true)
    public RateLimitManager rateLimitManager(RateLimitProperties properties) {
        log.info("Creating default RateLimitManager with expireAfterAccess: {} hours", properties.getExpireAfterAccess());
        return new GuavaRateLimitManager(properties.getExpireAfterAccess());
    }

    /**
     * 提供Redis的限流管理器
     *
     * @return RateLimitManager实例
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitManager.class)
    @ConditionalOnProperty(prefix = "ratelimit", name = "provider", havingValue = "redis")
    @ConditionalOnBean(RedisTemplate.class)
    public RateLimitManager redisRateLimitManager(RedisTemplate<Object, Object> redisTemplate, RateLimitProperties properties) {
        log.info("Creating Redis RateLimitManager with expireAfterAccess: {} hours", properties.getExpireAfterAccess());
        return new RedisRateLimitManager(redisTemplate, properties);
    }

    /**
     * IP类型限流key提供者
     */
    @Bean
    @ConditionalOnMissingBean(name = "ipBasedRateLimitKeyProvider")
    @ConditionalOnProperty(prefix = "ratelimit", name = "ip-based-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitKeyProvider ipBasedRateLimitKeyProvider() {
        return new IpBasedRateLimitKeyProvider();
    }

    /**
     * 用户类型限流key提供者
     */
    @Bean
    @ConditionalOnMissingBean(name = "userBasedRateLimitKeyProvider")
    @ConditionalOnProperty(prefix = "ratelimit", name = "user-based-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitKeyProvider userBasedRateLimitKeyProvider() {
        return new UserBasedRateLimitKeyProvider();
    }
}
