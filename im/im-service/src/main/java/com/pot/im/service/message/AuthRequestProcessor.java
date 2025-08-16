package com.pot.im.service.message;

import com.pot.im.service.protocol.serializer.MessageType;
import com.pot.im.service.protocol.serializer.ProtocolMessage;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author: Pot
 * @created: 2025/8/15 23:47
 * @description: 登录验证消息处理器
 */
public class AuthRequestProcessor implements MessageProcessor {

    @Override
    public void process(ChannelHandlerContext ctx, ProtocolMessage message) throws ProcessingException {
        // 处理登录验证逻辑
        byte[] data = message.getData();
        if (data == null || data.length == 0) {
            throw new ProcessingException("Authentication data is empty", null);
        }
        // todo 验证处理
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
            ctx.close(); // 关闭连接
        }
    }

    @Override
    public MessageType[] getSupportedTypes() {
        return new MessageType[0];
    }
}
