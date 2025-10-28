package com.pot.zing.framework.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Jackson JSON工具类
 *
 * <p>提供线程安全的JSON序列化和反序列化功能，支持：
 * <ul>
 *   <li>对象与JSON字符串互转</li>
 *   <li>对象与字节数组互转</li>
 *   <li>复杂类型转换</li>
 *   <li>流式处理</li>
 * </ul>
 *
 * @author: Pot
 * @created: 2025/3/16 22:41
 * @description: Jackson工具类
 */
@Slf4j
public final class JacksonUtils {

    /**
     * 默认日期格式
     */
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认时区
     */
    private static final String DEFAULT_TIMEZONE = "GMT+8";

    /**
     * 线程安全的ObjectMapper实例
     */
    private static final ObjectMapper MAPPER;

    static {
        MAPPER = createDefaultObjectMapper();
    }

    /**
     * 私有构造函数，防止实例化
     */
    private JacksonUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 创建默认配置的ObjectMapper
     */
    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册Java8时间模块
        mapper.registerModule(new JavaTimeModule());

        // 反序列化配置
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        // 序列化配置
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 日期格式配置
        SimpleDateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIMEZONE));
        mapper.setDateFormat(dateFormat);

        return mapper;
    }

    /**
     * 获取ObjectMapper实例
     *
     * @return ObjectMapper实例
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * 对象转JSON字符串
     *
     * @param obj 待转换对象
     * @return JSON字符串
     * @throws JsonSerializationException 序列化异常
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", obj.getClass().getSimpleName(), e);
            throw new JsonSerializationException("JSON序列化失败", e);
        }
    }

    /**
     * 对象转字节数组
     *
     * @param obj 待转换对象
     * @return 字节数组
     * @throws JsonSerializationException 序列化异常
     */
    public static byte[] toBytes(Object obj) {
        if (obj == null) {
            return new byte[0];
        }

        try {
            return MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to bytes: {}", obj.getClass().getSimpleName(), e);
            throw new JsonSerializationException("字节序列化失败", e);
        }
    }

    /**
     * JSON字符串转对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化对象
     * @throws JsonDeserializationException 反序列化异常
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to {}: {}", clazz.getSimpleName(), json, e);
            throw new JsonDeserializationException("JSON反序列化失败", e);
        }
    }

    /**
     * JSON字符串转复杂类型对象
     *
     * @param json          JSON字符串
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 反序列化对象
     * @throws JsonDeserializationException 反序列化异常
     */
    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to TypeReference: {}", json, e);
            throw new JsonDeserializationException("JSON反序列化失败", e);
        }
    }

    /**
     * 字节数组转对象
     *
     * @param data  字节数组
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化对象
     * @throws JsonDeserializationException 反序列化异常
     */
    public static <T> T toObject(byte[] data, Class<T> clazz) {
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            return MAPPER.readValue(data, clazz);
        } catch (IOException e) {
            log.error("Failed to deserialize bytes to {}", clazz.getSimpleName(), e);
            throw new JsonDeserializationException("字节反序列化失败", e);
        }
    }

    /**
     * 字节数组转复杂类型对象
     *
     * @param data          字节数组
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 反序列化对象
     * @throws JsonDeserializationException 反序列化异常
     */
    public static <T> T toObject(byte[] data, TypeReference<T> typeReference) {
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            return MAPPER.readValue(data, typeReference);
        } catch (IOException e) {
            log.error("Failed to deserialize bytes to TypeReference", e);
            throw new JsonDeserializationException("字节反序列化失败", e);
        }
    }

    /**
     * 输入流转对象
     *
     * @param inputStream 输入流
     * @param clazz       目标类型
     * @param <T>         泛型类型
     * @return 反序列化对象
     * @throws JsonDeserializationException 反序列化异常
     */
    public static <T> T toObject(InputStream inputStream, Class<T> clazz) {
        if (inputStream == null) {
            return null;
        }

        try {
            return MAPPER.readValue(inputStream, clazz);
        } catch (IOException e) {
            log.error("Failed to deserialize InputStream to {}", clazz.getSimpleName(), e);
            throw new JsonDeserializationException("流反序列化失败", e);
        }
    }

    /**
     * 解析JSON为JsonNode
     *
     * @param json JSON字符串
     * @return JsonNode对象
     * @throws JsonDeserializationException 解析异常
     */
    public static JsonNode parseTree(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON tree: {}", json, e);
            throw new JsonDeserializationException("JSON树解析失败", e);
        }
    }

    /**
     * 对象深拷贝
     *
     * @param obj   源对象
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 拷贝对象
     */
    public static <T> T deepCopy(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }

        try {
            String json = MAPPER.writeValueAsString(obj);
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deep copy object", e);
            throw new JsonSerializationException("对象深拷贝失败", e);
        }
    }

    /**
     * 验证JSON格式是否有效
     *
     * @param json JSON字符串
     * @return 是否有效
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        try {
            MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * JSON序列化异常
     */
    public static class JsonSerializationException extends RuntimeException {
        public JsonSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * JSON反序列化异常
     */
    public static class JsonDeserializationException extends RuntimeException {
        public JsonDeserializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}