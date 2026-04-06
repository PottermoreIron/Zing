package com.pot.im.service.protocol.serializer;

import com.pot.zing.framework.common.util.JacksonUtils;

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
