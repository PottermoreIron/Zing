package com.pot.im.service.client;

import com.pot.im.service.server.IMClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/8/11 23:14
 * @description: 客户端使用示例
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClientExample implements CommandLineRunner {

    private final IMClient imClient;

    @Override
    public void run(String... args) throws Exception {
        // 连接服务器
        boolean connected = imClient.connect().get();
        if (connected) {
            log.info("Client connected successfully");

            // 启用自动重连
            imClient.enableAutoReconnect();

            // 认证
            imClient.authenticate("user123", "token456").get();

            // 发送消息
            imClient.sendPrivateMessage("user456", "Hello, World!").get();

            // 保持连接
            Thread.sleep(30000);

            // 断开连接
            imClient.disconnect().get();
        }
    }
}
