package com.pot.im.service.protocol.serializer;

import com.pot.common.utils.JacksonUtils;

/**
 * @author: Pot
 * @created: 2025/8/10 17:10
 * @description: 自定义报文Json序列化实现
 */
public class JsonSerializer implements Serializer {

    @Override
    public byte getType() {
        return SerializerType.JSON.getCode();
    }

    @Override
    public <T> byte[] serialize(T obj) throws Exception {
        return JacksonUtils.toBytes(obj);
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) throws Exception {
        return JacksonUtils.toObject(data, clazz);
    }
}
