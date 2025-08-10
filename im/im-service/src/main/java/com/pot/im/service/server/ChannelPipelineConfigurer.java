package com.pot.im.service.server;

import com.pot.im.service.protocol.serializer.ProtocolDecoder;
import com.pot.im.service.protocol.serializer.ProtocolEncoder;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/8/10 23:24
 * @description: 通道管道配置器
 */
@Component
@RequiredArgsConstructor
public class ChannelPipelineConfigurer {

    private final IMServer.ServerConfig config;
    private final IMServerHandler serverHandler;

    public void configure(ChannelPipeline pipeline) {
        // 心跳检测
        pipeline.addLast("idle", new IdleStateHandler(
                config.getReaderIdleTime(), 0, 0, TimeUnit.SECONDS));

        // 协议编解码
        pipeline.addLast("decoder", new ProtocolDecoder());
        pipeline.addLast("encoder", new ProtocolEncoder());

        // 业务处理
        pipeline.addLast("handler", serverHandler);
    }
}
