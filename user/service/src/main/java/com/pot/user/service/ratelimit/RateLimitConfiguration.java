package com.pot.user.service.ratelimit;

import com.pot.user.service.ratelimit.impl.IpBasedRateLimitKeyProvider;
import com.pot.user.service.ratelimit.impl.UserBasedRateLimitKeyProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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
