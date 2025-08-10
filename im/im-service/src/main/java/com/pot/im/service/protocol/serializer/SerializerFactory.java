package com.pot.im.service.protocol.serializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Pot
 * @created: 2025/8/10 17:14
 * @description: 序列化器工厂
 */
public class SerializerFactory {
    private static final Map<Byte, Serializer> map = new ConcurrentHashMap<>();

    static {
        map.put(SerializerType.JSON.getCode(), new JsonSerializer());
    }

    public static Serializer getSerializer(byte type) {
        return map.get(type);
    }
}
