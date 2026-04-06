package com.pot.im.service.message;

import com.pot.im.service.protocol.serializer.MessageType;
import com.pot.im.service.protocol.serializer.ProtocolMessage;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HeartbeatProcessor implements MessageProcessor {

    @Override
    public void process(ChannelHandlerContext ctx, ProtocolMessage message) {
        ProtocolMessage response = new ProtocolMessage();
        response.getHeader().setMsgType(MessageType.HEARTBEAT_ACK.getCode());
        response.getHeader().setSequence(message.getHeader().getSequence());
        response.getHeader().setTimestamp(System.currentTimeMillis());
        response.setData(new byte[0]);

        ctx.writeAndFlush(response);
    }

    @Override
    public MessageType[] getSupportedTypes() {
        return new MessageType[]{MessageType.HEARTBEAT};
    }
}
