package com.pot.user.service.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;

/**
 * @author: Pot
 * @created: 2025/3/16 22:41
 * @description: Jackson工具类
 */
public class JacksonUtils {
    private final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final static ObjectMapper MAPPER = new ObjectMapper();

    static {
        // 忽略未知属性
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 忽略序列化时的空属性
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        // 设置日期格式
        MAPPER.setDateFormat(new SimpleDateFormat(DATE_FORMAT));
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T toObject(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
