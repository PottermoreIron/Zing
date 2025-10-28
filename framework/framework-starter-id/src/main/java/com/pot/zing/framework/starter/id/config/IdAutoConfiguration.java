package com.pot.zing.framework.starter.id.config;

import com.pot.zing.framework.starter.id.properties.IdProperties;
import com.pot.zing.framework.starter.id.service.IdGenerator;
import com.pot.zing.framework.starter.id.service.impl.IdServiceImpl;
import com.pot.zing.framework.starter.id.service.impl.LeafIdGeneratorImpl;
import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.segment.SegmentIDGenImpl;
import com.sankuai.inf.leaf.segment.dao.IDAllocDao;
import com.sankuai.inf.leaf.segment.dao.impl.IDAllocDaoImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * @author: Pot
 * @created: 2025/10/18 23:25
 * @description: 自定义分布式Id生成自动装配类
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(IdGenerator.class)
@ConditionalOnProperty(prefix = "pot.id", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(IdProperties.class)
public class IdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "pot.id.leaf", name = "segment-enabled", havingValue = "true", matchIfMissing = true)
    public IDAllocDao potIdAllocDao(DataSource dataSource) {
        return new IDAllocDaoImpl(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "pot.id.leaf", name = "segment-enabled", havingValue = "true", matchIfMissing = true)
    public IDGen potSegmentIdGen(IDAllocDao idAllocDao) {
        SegmentIDGenImpl segmentIDGen = new SegmentIDGenImpl();
        segmentIDGen.setDao(idAllocDao);
        if (!segmentIDGen.init()) {
            throw new IllegalStateException("Segment ID generator initialization failed");
        }
        log.info("Pot Segment ID generator initialized successfully");
        return segmentIDGen;
    }

    @Bean
    @ConditionalOnMissingBean
    public IdGenerator potIdGenerator(IDGen segmentIdGen) {
        log.info("Creating PotLeafIdGenerator");
        return new LeafIdGeneratorImpl(segmentIdGen);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdServiceImpl potIdService(IdGenerator idGenerator) {
        log.info("Creating PotIdService");
        return new IdServiceImpl(idGenerator);
    }
}
