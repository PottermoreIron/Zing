package com.pot.im.service.protocol.serializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SerializerFactory {
    private static final Map<Byte, Serializer> map = new ConcurrentHashMap<>();

    static {
        map.put(SerializerType.JSON.getCode(), new JsonSerializer());
    }

    public static Serializer getSerializer(byte type) {
        return map.get(type);
    }
}
