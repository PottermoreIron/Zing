package com.pot.im.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/8/16 20:20
 * @description: 服务器配置
 */
@ConfigurationProperties(prefix = "im.server")
@Component
@Data
public class ServerConfig {
    private int port = 8888;
    private int bossThreads = 1;
    private int workerThreads = Runtime.getRuntime().availableProcessors();
    private int backlog = 1024;
    private int readerIdleTime = 60;
}
