package com.pot.im.service.protocol.serializer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: Pot
 * @created: 2025/8/10 17:05
 * @description: 自定义报文序列化枚举
 */
@AllArgsConstructor
public enum SerializerType {
    JSON((byte) 1),
    PROTOBUF((byte) 2);

    @Getter
    private final byte code;

    public static SerializerType fromCode(byte code) {
        for (SerializerType type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("Unknown serializer type: " + code);
    }
}
