package com.pot.zing.framework.starter.touch.config;

import com.pot.zing.framework.starter.touch.properties.TouchProperties;
import com.pot.zing.framework.starter.touch.service.TouchChannel;
import com.pot.zing.framework.starter.touch.service.impl.TouchServiceImpl;
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
 * Auto-configuration for touch delivery beans.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(TouchProperties.class)
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

}
