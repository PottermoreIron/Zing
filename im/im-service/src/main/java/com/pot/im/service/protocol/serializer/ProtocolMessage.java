package com.pot.im.service.protocol.serializer;

import lombok.Data;


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
