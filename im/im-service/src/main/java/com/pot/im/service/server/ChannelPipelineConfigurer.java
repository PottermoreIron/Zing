package com.pot.im.service.server;

import com.pot.im.service.config.ServerConfig;
import com.pot.im.service.protocol.serializer.ProtocolDecoder;
import com.pot.im.service.protocol.serializer.ProtocolEncoder;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ChannelPipelineConfigurer {

    private final ServerConfig config;
    private final IMServerHandler serverHandler;

    public void configure(ChannelPipeline pipeline) {
        pipeline.addLast("idle", new IdleStateHandler(
                config.getReaderIdleTime(), 0, 0, TimeUnit.SECONDS));

        pipeline.addLast("decoder", new ProtocolDecoder());
        pipeline.addLast("encoder", new ProtocolEncoder());

        pipeline.addLast("handler", serverHandler);
    }
}
