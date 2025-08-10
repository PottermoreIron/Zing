package com.pot.im.service.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/8/10 23:00
 * @description: IM服务器核心启动类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IMServer implements ApplicationRunner {

    private final ServerConfig config;
    private final ChannelPipelineConfigurer pipelineConfigurer;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        startServer();
    }

    private void startServer() {
        bossGroup = new NioEventLoopGroup(config.getBossThreads());
        workerGroup = new NioEventLoopGroup(config.getWorkerThreads());

        try {
            ServerBootstrap bootstrap = createServerBootstrap();
            ChannelFuture future = bootstrap.bind(config.getPort()).sync();

            serverChannel = future.channel();
            log.info("IM Server started successfully on port {}", config.getPort());

            future.channel().closeFuture().addListener(f ->
                    log.info("IM Server stopped gracefully"));

        } catch (InterruptedException e) {
            log.error("Failed to start IM Server", e);
            Thread.currentThread().interrupt();
            shutdown();
        }
    }

    private ServerBootstrap createServerBootstrap() {
        return new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, config.getBacklog())
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        pipelineConfigurer.configure(ch.pipeline());
                    }
                });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down IM Server...");

        closeChannel(serverChannel);
        shutdownEventLoopGroup(workerGroup, "Worker");
        shutdownEventLoopGroup(bossGroup, "Boss");

        log.info("IM Server shutdown completed");
    }

    private void closeChannel(Channel channel) {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    private void shutdownEventLoopGroup(EventLoopGroup group, String name) {
        if (group != null && !group.isShutdown()) {
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS)
                    .addListener(future -> log.info("{} EventLoopGroup shutdown", name));
        }
    }

    @ConfigurationProperties(prefix = "im.server")
    @Component
    @lombok.Data
    public static class ServerConfig {
        private int port = 8888;
        private int bossThreads = 1;
        private int workerThreads = Runtime.getRuntime().availableProcessors();
        private int backlog = 1024;
        private int readerIdleTime = 60;
    }
}
