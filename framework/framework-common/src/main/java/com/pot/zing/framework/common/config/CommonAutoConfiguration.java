package com.pot.zing.framework.common.config;

import com.pot.zing.framework.common.properties.JwtProperties;
import com.pot.zing.framework.common.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author: Pot
 * @created: 2025/10/19 20:59
 * @description: common自动配置类
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class CommonAutoConfiguration {

    public CommonAutoConfiguration() {
        log.info("加载 Common 自动配置");
    }

    /**
     * JWT工具类配置
     * 只有在类路径中存在 JWT 相关类时才加载
     * 可通过 jwt.enabled=false 禁用
     */
    @Bean
    @ConditionalOnClass(name = "io.jsonwebtoken.Jwts")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "pot.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JwtUtils jwtUtils(JwtProperties jwtProperties) {
        log.info("初始化 JwtUtils, issuer: {}", jwtProperties.getIssuer());
        return new JwtUtils(jwtProperties);
    }
}
