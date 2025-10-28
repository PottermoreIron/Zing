package com.pot.zing.framework.starter.code.generator.config;

import com.pot.zing.framework.starter.code.generator.properties.CodeGeneratorProperties;
import com.pot.zing.framework.starter.code.generator.service.CodeGenerationService;
import com.pot.zing.framework.starter.code.generator.service.impl.DefaultCodeGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author: Pot
 * @created: 2025/10/18 21:36
 * @description: 代码生成器自动装配类
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(CodeGeneratorProperties.class)
@ConditionalOnProperty(prefix = "pot.code.generator", name = "enabled", havingValue = "true", matchIfMissing = false)
public class CodeGeneratorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CodeGenerationService potCodeGenerationService(CodeGeneratorProperties properties) {
        log.info("🚀 初始化代码生成器服务 - 数据库: {}, 包名: {}",
                properties.getDatabase(), properties.getBasePackage());
        return new DefaultCodeGenerationService(properties);
    }
}
