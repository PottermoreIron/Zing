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
 * Auto-configuration for the code generator starter.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(CodeGeneratorProperties.class)
@ConditionalOnProperty(prefix = "pot.code.generator", name = "enabled", havingValue = "true", matchIfMissing = false)
public class CodeGeneratorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CodeGenerationService potCodeGenerationService(CodeGeneratorProperties properties) {
        log.info("Initializing code generator — datasource: {}, base package: {}",
                properties.getDatabase(), properties.getBasePackage());
        return new DefaultCodeGenerationService(properties);
    }
}
