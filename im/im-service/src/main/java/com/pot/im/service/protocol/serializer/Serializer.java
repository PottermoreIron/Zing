package com.pot.im.service.protocol.serializer;

public interface Serializer {
        byte getType();

        <T> byte[] serialize(T obj) throws Exception;

        <T> T deserialize(byte[] data, Class<T> clazz) throws Exception;
}
