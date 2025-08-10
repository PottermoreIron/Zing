package com.pot.im.service.protocol.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author: Pot
 * @created: 2025/8/10 17:10
 * @description: 自定义报文Json序列化实现
 */
public class JsonSerializer implements Serializer {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public byte getType() {
        return SerializerType.JSON.getCode();
    }

    @Override
    public <T> byte[] serialize(T obj) throws Exception {
        return mapper.writeValueAsBytes(obj);
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) throws Exception {
        return mapper.readValue(data, clazz);
    }
}
