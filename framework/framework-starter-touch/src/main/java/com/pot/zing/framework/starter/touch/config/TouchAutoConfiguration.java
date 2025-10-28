package com.pot.zing.framework.starter.touch.config;

import com.pot.zing.framework.starter.redis.service.RedisService;
import com.pot.zing.framework.starter.touch.properties.TouchProperties;
import com.pot.zing.framework.starter.touch.properties.VerificationCodeProperties;
import com.pot.zing.framework.starter.touch.service.TouchChannel;
import com.pot.zing.framework.starter.touch.service.VerificationCodeService;
import com.pot.zing.framework.starter.touch.service.impl.TouchServiceImpl;
import com.pot.zing.framework.starter.touch.service.impl.VerificationCodeServiceImpl;
import com.pot.zing.framework.starter.touch.strategy.ChannelSelectionStrategy;
import com.pot.zing.framework.starter.touch.strategy.impl.DefaultChannelSelectionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author: Pot
 * @created: 2025/10/19 16:01
 * @description: 触达自动装配类
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({TouchProperties.class, VerificationCodeProperties.class})
@ConditionalOnProperty(prefix = "pot.touch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TouchAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ChannelSelectionStrategy channelSelectionStrategy() {
        return new DefaultChannelSelectionStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    public TouchServiceImpl touchService(
            List<TouchChannel> channels,
            ChannelSelectionStrategy selectionStrategy) {
        log.info("初始化 TouchService, 发现 {} 个渠道实现", channels.size());
        return new TouchServiceImpl(channels, selectionStrategy);
    }

    @Bean
    @ConditionalOnMissingBean
    public VerificationCodeService verificationCodeService(
            TouchServiceImpl touchService,
            RedisService redisService,
            TouchProperties touchProperties,
            VerificationCodeProperties verificationCodeProperties) {
        log.info("初始化 VerificationCodeService");
        return new VerificationCodeServiceImpl(
                touchService,
                redisService,
                touchProperties,
                verificationCodeProperties
        );
    }
}
