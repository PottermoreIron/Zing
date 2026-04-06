package com.pot.im.service.message;

import com.pot.im.service.protocol.serializer.MessageType;
import com.pot.im.service.protocol.serializer.ProtocolMessage;
import io.netty.channel.ChannelHandlerContext;

/**
 * Processes client authentication messages.
 */
public class AuthRequestProcessor implements MessageProcessor {

    @Override
    public void process(ChannelHandlerContext ctx, ProtocolMessage message) throws ProcessingException {
        byte[] data = message.getData();
        if (data == null || data.length == 0) {
            throw new ProcessingException("Authentication data is empty", null);
        }

        // Authentication verification is not wired yet, so requests are rejected by
        // default.
        boolean isAuthenticated = false;
        ProtocolMessage response = new ProtocolMessage();
        response.getHeader().setMsgType(MessageType.AUTH_RESPONSE.getCode());
        response.getHeader().setSequence(message.getHeader().getSequence());
        response.getHeader().setTimestamp(System.currentTimeMillis());

        if (isAuthenticated) {
            response.setData("Authentication successful".getBytes());
            ctx.writeAndFlush(response);
        } else {
            response.setData("Authentication failed".getBytes());
            ctx.writeAndFlush(response);
            ctx.close();
        }
    }

    @Override
    public MessageType[] getSupportedTypes() {
        return new MessageType[0];
    }
}
