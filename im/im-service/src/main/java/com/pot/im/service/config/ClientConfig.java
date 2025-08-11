package com.pot.im.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/8/11 23:10
 * @description: IM客户端配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "im.client")
public class ClientConfig {
    private String serverHost = "127.0.0.1";
    private int serverPort = 20000;
    private int connectTimeoutMs = 5000;
    private int readerIdleTime = 70;
    private int writerIdleTime = 50;
    private int reconnectDelayMs = 3000;
    private int maxReconnectTimes = 5;
}
