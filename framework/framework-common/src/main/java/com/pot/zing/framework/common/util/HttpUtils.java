package com.pot.zing.framework.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: Pot
 * @created: 2025/2/22 16:48
 * @description: 网络工具类
 */
public class HttpUtils {
    /**
     * 解析请求体
     *
     * @param request 请求
     * @return 请求体
     * @throws IOException 异常
     */
    public static Map<String, Object> parseJsonRequest(HttpServletRequest request) throws IOException {
        // 使用 Jackson 解析请求体
        return JacksonUtils.toObject(request.getReader().lines().collect(Collectors.joining()), new TypeReference<>() {
        });
    }

    /**
     * 获取请求参数
     *
     * @param param       参数名
     * @param requestJson 请求体
     * @param <T>         返回值类型
     * @return 返回值
     */
    public static <T> T obtainParamValue(String param, Map<String, Object> requestJson, Class<T> clazz) {
        return Optional.ofNullable(requestJson.get(param))
                .map(clazz::cast)
                .orElse(null);
    }

    public static HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
