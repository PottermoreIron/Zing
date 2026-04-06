package com.pot.im.service.client;

import com.pot.im.service.config.ClientConfig;
import com.pot.im.service.protocol.serializer.MessageType;
import com.pot.im.service.protocol.serializer.ProtocolMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class IMClient {

    private final ClientConfig config;
    private final ClientChannelInitializer channelInitializer;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger reconnectCount = new AtomicInteger(0);
    private EventLoopGroup eventLoopGroup;
    private Channel channel;
    private ScheduledExecutorService scheduledExecutor;

        public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                initializeResources();
                Bootstrap bootstrap = createBootstrap();

                ChannelFuture future = bootstrap.connect(config.getServerHost(), config.getServerPort())
                        .await();

                if (future.isSuccess()) {
                    channel = future.channel();
                    connected.set(true);
                    reconnectCount.set(0);
                    log.info("Connected to IM Server {}:{}", config.getServerHost(), config.getServerPort());
                    return true;
                } else {
                    log.error("Failed to connect to IM Server", future.cause());
                    return false;
                }
            } catch (Exception e) {
                log.error("Connection error", e);
                return false;
            }
        });
    }

        public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            connected.set(false);
            closeChannel();
            shutdownResources();
            log.info("Disconnected from IM Server");
        });
    }

        public CompletableFuture<Boolean> sendMessage(ProtocolMessage message) {
        if (!isConnected()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                ChannelFuture future = channel.writeAndFlush(message).await();
                return future.isSuccess();
            } catch (Exception e) {
                log.error("Failed to send message", e);
                return false;
            }
        });
    }

        public CompletableFuture<Boolean> authenticate(String userId, String token) {
        String authData = userId + ":" + token;
        ProtocolMessage authMessage = new ProtocolMessage(MessageType.AUTH_REQUEST, authData.getBytes());
        return sendMessage(authMessage);
    }

        public CompletableFuture<Boolean> sendHeartbeat() {
        ProtocolMessage heartbeat = new ProtocolMessage(MessageType.HEARTBEAT, new byte[0]);
        return sendMessage(heartbeat);
    }

        public CompletableFuture<Boolean> sendPrivateMessage(String targetUserId, String content) {
        String messageData = targetUserId + ":" + content;
        ProtocolMessage message = new ProtocolMessage(MessageType.PRIVATE_MESSAGE, messageData.getBytes());
        return sendMessage(message);
    }

        public void enableAutoReconnect() {
        if (scheduledExecutor == null) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "IMClient-Reconnect");
                t.setDaemon(true);
                return t;
            });
        }

        scheduledExecutor.scheduleWithFixedDelay(this::checkAndReconnect,
                config.getReconnectDelayMs(), config.getReconnectDelayMs(), TimeUnit.MILLISECONDS);
    }

        public boolean isConnected() {
        return connected.get() && channel != null && channel.isActive();
    }

    private void initializeResources() {
        if (eventLoopGroup == null) {
            eventLoopGroup = new NioEventLoopGroup(1);
        }
    }

    private Bootstrap createBootstrap() {
        return new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeoutMs())
                .handler(channelInitializer);
    }

    private void checkAndReconnect() {
        if (!isConnected() && reconnectCount.get() < config.getMaxReconnectTimes()) {
            log.info("Attempting to reconnect... ({})", reconnectCount.incrementAndGet());
            connect().thenAccept(success -> {
                if (!success) {
                    log.warn("Reconnection failed, will retry later");
                }
            });
        }
    }

    private void closeChannel() {
        if (channel != null && channel.isOpen()) {
            channel.close().addListener(future ->
                    log.debug("Channel closed successfully"));
        }
    }

    @PreDestroy
    private void shutdownResources() {
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            scheduledExecutor.shutdown();
        }

        if (eventLoopGroup != null && !eventLoopGroup.isShutdown()) {
            eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS)
                    .addListener(future -> log.debug("EventLoopGroup shutdown completed"));
        }
    }
}
