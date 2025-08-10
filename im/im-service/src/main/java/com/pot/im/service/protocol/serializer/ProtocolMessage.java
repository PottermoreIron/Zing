package com.pot.im.service.protocol.serializer;

import lombok.Data;

/**
 * @author: Pot
 * @created: 2025/8/10 22:44
 * @description: 自定义消息
 */

@Data
public class ProtocolMessage {
    private ProtocolHeader header;
    private byte[] data;

    public ProtocolMessage() {
        this.header = new ProtocolHeader();
    }

    public ProtocolMessage(MessageType messageType, byte[] data) {
        this();
        this.header.setMsgType(messageType.getCode());
        this.header.setTimestamp(System.currentTimeMillis());
        this.data = data != null ? data : new byte[0];
    }
}
