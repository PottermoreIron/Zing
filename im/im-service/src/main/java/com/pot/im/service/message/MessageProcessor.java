package com.pot.im.service.message;

import com.pot.im.service.protocol.serializer.MessageType;
import com.pot.im.service.protocol.serializer.ProtocolMessage;
import io.netty.channel.ChannelHandlerContext;

public interface MessageProcessor {

        void process(ChannelHandlerContext ctx, ProtocolMessage message) throws ProcessingException;

        MessageType[] getSupportedTypes();

        default int getPriority() {
        return Integer.MAX_VALUE;
    }

        default boolean isAsync() {
        return false;
    }

        class ProcessingException extends Exception {
        public ProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
