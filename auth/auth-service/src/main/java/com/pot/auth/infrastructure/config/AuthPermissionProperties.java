package com.pot.auth.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Auth 权限系统配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.permission")
public class AuthPermissionProperties {

    private Cache cache = new Cache();

    @Data
    public static class Cache {
        private long redisTtl = 3600;
        private long localTtl = 300;
        private int localMaxSize = 10000;
        private long bloomCapacity = 100000;
        private double bloomFpp = 0.01D;
        private boolean versionEnabled = true;
    }
}