package com.pot.im.service.server;

import com.pot.im.service.config.ClientConfig;
import com.pot.im.service.protocol.serializer.ProtocolDecoder;
import com.pot.im.service.protocol.serializer.ProtocolEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/8/11 23:11
 * @description: 客户端channel初始化器
 */
@Component
@RequiredArgsConstructor
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final ClientConfig config;
    private final IMClientHandler clientHandler;

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // 心跳检测
        pipeline.addLast(new IdleStateHandler(
                config.getReaderIdleTime(),
                config.getWriterIdleTime(),
                0,
                TimeUnit.SECONDS));

        // 协议编解码器
        pipeline.addLast(new ProtocolDecoder());
        pipeline.addLast(new ProtocolEncoder());

        // 客户端业务处理器
        pipeline.addLast(clientHandler);
    }
}
