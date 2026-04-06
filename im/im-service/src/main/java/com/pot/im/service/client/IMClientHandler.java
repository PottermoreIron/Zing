package com.pot.im.service.client;

import com.pot.im.service.protocol.serializer.MessageType;
import com.pot.im.service.protocol.serializer.ProtocolMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles inbound messages and connection lifecycle events for the sample IM
 * client.
 */
@Component
@Slf4j
public class IMClientHandler extends SimpleChannelInboundHandler<ProtocolMessage> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Connected to server: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("Disconnected from server: {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtocolMessage msg) {
        try {
            MessageType messageType = MessageType.fromCode(msg.getHeader().getMsgType());

            switch (messageType) {
                case HEARTBEAT_ACK:
                    log.debug("Received heartbeat ack");
                    break;
                case AUTH_RESPONSE:
                    handleAuthResponse(msg);
                    break;
                case PRIVATE_MESSAGE:
                    handlePrivateMessage(msg);
                    break;
                case GROUP_MESSAGE:
                    handleGroupMessage(msg);
                    break;
                default:
                    log.warn("Unhandled message type: {}", messageType);
            }
        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent event) {
            if (event.state() == IdleState.WRITER_IDLE) {
                ProtocolMessage heartbeat = new ProtocolMessage(MessageType.HEARTBEAT, new byte[0]);
                ctx.writeAndFlush(heartbeat);
                log.debug("Sent heartbeat to server");
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Client handler exception", cause);
        ctx.close();
    }

    private void handleAuthResponse(ProtocolMessage msg) {
        String response = new String(msg.getData());
        if ("AUTH_SUCCESS".equals(response)) {
            log.info("Authentication successful");
        } else {
            log.warn("Authentication failed: {}", response);
        }
    }

    private void handlePrivateMessage(ProtocolMessage msg) {
        String messageContent = new String(msg.getData());
        log.info("Received private message: {}", messageContent);
        // Hook for application-specific private message handling.
    }

    private void handleGroupMessage(ProtocolMessage msg) {
        String messageContent = new String(msg.getData());
        log.info("Received group message: {}", messageContent);
        // Hook for application-specific group message handling.
    }
}
