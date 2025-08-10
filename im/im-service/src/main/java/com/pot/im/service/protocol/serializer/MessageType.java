package com.pot.im.service.protocol.serializer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: Pot
 * @created: 2025/8/10 22:21
 * @description: IM消息类型
 */
@AllArgsConstructor
@Getter
public enum MessageType {
    // 连接管理
    CONNECT_REQUEST((byte) 0x01),
    CONNECT_RESPONSE((byte) 0x02),
    DISCONNECT((byte) 0x03),
    HEARTBEAT((byte) 0x04),
    HEARTBEAT_ACK((byte) 0x05),

    // 用户认证
    AUTH_REQUEST((byte) 0x10),
    AUTH_RESPONSE((byte) 0x11),

    // 单聊消息
    PRIVATE_MESSAGE((byte) 0x20),
    PRIVATE_MESSAGE_ACK((byte) 0x21),

    // 群聊消息
    GROUP_MESSAGE((byte) 0x30),
    GROUP_MESSAGE_ACK((byte) 0x31),

    // 系统消息
    SYSTEM_MESSAGE((byte) 0x40),

    // 文件传输
    FILE_UPLOAD_REQUEST((byte) 0x50),
    FILE_UPLOAD_RESPONSE((byte) 0x51),
    FILE_DOWNLOAD_REQUEST((byte) 0x52),
    FILE_DOWNLOAD_RESPONSE((byte) 0x53),

    // 错误消息
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
