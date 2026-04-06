package com.pot.im.service.client;

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
 * Configures the Netty pipeline for the sample IM client.
 */
@Component
@RequiredArgsConstructor
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final ClientConfig config;
    private final IMClientHandler clientHandler;

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new IdleStateHandler(
                config.getReaderIdleTime(),
                config.getWriterIdleTime(),
                0,
                TimeUnit.SECONDS));

        pipeline.addLast(new ProtocolDecoder());
        pipeline.addLast(new ProtocolEncoder());

        pipeline.addLast(clientHandler);
    }
}
