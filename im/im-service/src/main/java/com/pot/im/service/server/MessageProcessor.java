package com.pot.im.service.server;

import com.pot.im.service.protocol.serializer.MessageType;
import com.pot.im.service.protocol.serializer.ProtocolMessage;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author: Pot
 * @created: 2025/8/10 23:09
 * @description: 消息处理器接口
 */
public interface MessageProcessor {

    /**
     * 处理消息
     *
     * @param ctx     通道上下文
     * @param message 协议消息
     * @throws ProcessingException 处理异常
     */
    void process(ChannelHandlerContext ctx, ProtocolMessage message) throws ProcessingException;

    /**
     * 获取支持的消息类型
     *
     * @return 支持的消息类型数组
     */
    MessageType[] getSupportedTypes();

    /**
     * 处理器优先级 (数值越小优先级越高)
     *
     * @return 优先级值
     */
    default int getPriority() {
        return Integer.MAX_VALUE;
    }

    /**
     * 是否支持异步处理
     *
     * @return true表示支持异步处理
     */
    default boolean isAsync() {
        return false;
    }

    /**
     * 消息处理异常
     */
    class ProcessingException extends Exception {
        public ProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
