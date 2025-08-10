package com.pot.im.service.protocol.serializer;

/**
 * @author: Pot
 * @created: 2025/8/10 17:03
 * @description: 自定义报文序列化接口
 */
public interface Serializer {
    /**
     * 获取序列化类型
     *
     * @return byte
     * @author pot
     * @description 获取序列化类型
     * @date 17:08 2025/8/10
     **/
    byte getType();

    /**
     * 序列化对象为字节数组
     *
     * @param obj 需要序列化的对象
     * @return byte
     * @author pot
     * @description 序列化对象为字节数组
     * @date 17:08 2025/8/10
     **/
    <T> byte[] serialize(T obj) throws Exception;

    /**
     * 反序列化字节数组为对象
     *
     * @param data  字节数组
     * @param clazz 目标类类型
     * @return T
     * @author pot
     * @description 反序列化字节数组为对象
     * @date 17:08 2025/8/10
     **/
    <T> T deserialize(byte[] data, Class<T> clazz) throws Exception;
}
