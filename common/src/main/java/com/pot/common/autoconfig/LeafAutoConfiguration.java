package com.pot.common.autoconfig;

import com.pot.common.id.IdService;
import com.pot.common.id.impl.LeafIdServiceImpl;
import com.sankuai.inf.leaf.service.SegmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: Pot
 * @created: 2025/8/17 21:42
 * @description: Leaf自动配置类
 */
@Configuration
@ConditionalOnProperty(prefix = "leaf.segment", name = "enable", havingValue = "true")
@Slf4j
public class LeafAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdService.class)
    public IdService leafIdService(@Qualifier("initLeafSegmentStarter") SegmentService segmentService) {
        log.info("创建LeafIdServiceImpl bean");
        return new LeafIdServiceImpl(segmentService);
    }
}
