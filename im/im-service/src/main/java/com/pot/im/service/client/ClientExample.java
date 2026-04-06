package com.pot.im.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Demonstrates a basic IM client startup flow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClientExample implements CommandLineRunner {

    private final IMClient imClient;

    @Override
    public void run(String... args) throws Exception {
        boolean connected = imClient.connect().get();
        if (connected) {
            log.info("Client connected successfully");

            imClient.enableAutoReconnect();

            imClient.authenticate("user123", "token456").get();

            imClient.sendPrivateMessage("user456", "Hello, World!").get();

            Thread.sleep(30000);

            imClient.disconnect().get();
        }
    }
}
