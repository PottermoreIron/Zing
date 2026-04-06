package com.pot.im.service.protocol.serializer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MessageType {
    CONNECT_REQUEST((byte) 0x01),
    CONNECT_RESPONSE((byte) 0x02),
    DISCONNECT((byte) 0x03),
    HEARTBEAT((byte) 0x04),
    HEARTBEAT_ACK((byte) 0x05),

    AUTH_REQUEST((byte) 0x10),
    AUTH_RESPONSE((byte) 0x11),

    PRIVATE_MESSAGE((byte) 0x20),
    PRIVATE_MESSAGE_ACK((byte) 0x21),

    GROUP_MESSAGE((byte) 0x30),
    GROUP_MESSAGE_ACK((byte) 0x31),

    SYSTEM_MESSAGE((byte) 0x40),

    FILE_UPLOAD_REQUEST((byte) 0x50),
    FILE_UPLOAD_RESPONSE((byte) 0x51),
    FILE_DOWNLOAD_REQUEST((byte) 0x52),
    FILE_DOWNLOAD_RESPONSE((byte) 0x53),

    ERROR((byte) 0xFF);

    private final byte code;

    public static MessageType fromCode(byte code) {
        for (MessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type: " + code);
    }
}
