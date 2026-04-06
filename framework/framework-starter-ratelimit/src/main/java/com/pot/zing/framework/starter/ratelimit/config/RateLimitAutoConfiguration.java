package com.pot.zing.framework.starter.ratelimit.config;

import com.pot.zing.framework.starter.ratelimit.aspect.RateLimitAspect;
import com.pot.zing.framework.starter.ratelimit.properties.RateLimitProperties;
import com.pot.zing.framework.starter.ratelimit.service.RateLimitKeyProvider;
import com.pot.zing.framework.starter.ratelimit.service.RateLimitManager;
import com.pot.zing.framework.starter.ratelimit.service.impl.*;
import com.pot.zing.framework.starter.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-configuration for rate limiting.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "pot.ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAutoConfiguration {

    /**
     * Guava-backed rate-limit manager.
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitManager.class)
    @ConditionalOnProperty(prefix = "pot.ratelimit", name = "provider", havingValue = "guava", matchIfMissing = true)
    public RateLimitManager guavaRateLimitManager(RateLimitProperties properties) {
        log.info("初始化Pot Guava限流管理器 - expireAfterAccess: {} 小时", properties.getExpireAfterAccess());
        return new GuavaRateLimitManager(properties);
    }

    /**
     * Redis-backed rate-limit manager.
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitManager.class)
    @ConditionalOnProperty(prefix = "pot.ratelimit", name = "provider", havingValue = "redis")
    @ConditionalOnBean(RedisService.class)
    public RateLimitManager redisRateLimitManager(RedisService redisService, RateLimitProperties properties) {
        log.info("初始化Pot Redis限流管理器 - expireAfterAccess: {} 小时", properties.getExpireAfterAccess());
        return new RedisRateLimitManager(redisService, properties);
    }

    /**
     * Fixed key provider.
     */
    @Bean
    @ConditionalOnMissingBean(name = "potFixedRateLimitKeyProvider")
    public RateLimitKeyProvider potFixedRateLimitKeyProvider() {
        log.debug("注册Pot固定限流Key提供者");
        return new FixedRateLimitKeyProvider();
    }

    /**
     * IP-based key provider.
     */
    @Bean
    @ConditionalOnMissingBean(name = "potIpBasedRateLimitKeyProvider")
    @ConditionalOnProperty(prefix = "pot.ratelimit", name = "ip-based-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitKeyProvider potIpBasedRateLimitKeyProvider() {
        log.debug("注册Pot IP限流Key提供者");
        return new IpBasedRateLimitKeyProvider();
    }

    /**
     * User-based key provider.
     */
    @Bean
    @ConditionalOnMissingBean(name = "potUserBasedRateLimitKeyProvider")
    @ConditionalOnProperty(prefix = "pot.ratelimit", name = "user-based-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitKeyProvider potUserBasedRateLimitKeyProvider() {
        log.debug("注册Pot用户限流Key提供者");
        return new UserBasedRateLimitKeyProvider();
    }

    /**
     * Rate-limit aspect.
     */
    @Bean
    @ConditionalOnBean(RateLimitManager.class)
    public RateLimitAspect potRateLimitAspect(
            RateLimitManager rateLimitManager,
            List<RateLimitKeyProvider> keyProviders,
            RateLimitProperties properties) {
        log.info("初始化Pot限流切面 - 管理器类型: {}, Key提供者数量: {}",
                rateLimitManager.getType(), keyProviders.size());
        return new RateLimitAspect(rateLimitManager, keyProviders, properties);
    }
}
