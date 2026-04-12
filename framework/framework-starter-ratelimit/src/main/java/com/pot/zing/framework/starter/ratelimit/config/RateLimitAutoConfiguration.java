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
        log.info("Initializing Guava rate-limit manager — expireAfterAccess: {} h", properties.getExpireAfterAccess());
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
        log.info("Initializing Redis rate-limit manager — expireAfterAccess: {} h", properties.getExpireAfterAccess());
        return new RedisRateLimitManager(redisService, properties);
    }

    /**
     * Fixed key provider.
     */
    @Bean
    @ConditionalOnMissingBean(name = "potFixedRateLimitKeyProvider")
    public RateLimitKeyProvider potFixedRateLimitKeyProvider() {
        log.debug("Registering fixed-key rate-limit provider");
        return new FixedRateLimitKeyProvider();
    }

    /**
     * IP-based key provider.
     */
    @Bean
    @ConditionalOnMissingBean(name = "potIpBasedRateLimitKeyProvider")
    @ConditionalOnProperty(prefix = "pot.ratelimit", name = "ip-based-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitKeyProvider potIpBasedRateLimitKeyProvider() {
        log.debug("Registering IP-based rate-limit key provider");
        return new IpBasedRateLimitKeyProvider();
    }

    /**
     * User-based key provider.
     */
    @Bean
    @ConditionalOnMissingBean(name = "potUserBasedRateLimitKeyProvider")
    @ConditionalOnProperty(prefix = "pot.ratelimit", name = "user-based-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitKeyProvider potUserBasedRateLimitKeyProvider() {
        log.debug("Registering user-based rate-limit key provider");
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
        log.info("Initializing rate-limit aspect — manager: {}, key providers: {}",
                rateLimitManager.getType(), keyProviders.size());
        return new RateLimitAspect(rateLimitManager, keyProviders, properties);
    }
}
