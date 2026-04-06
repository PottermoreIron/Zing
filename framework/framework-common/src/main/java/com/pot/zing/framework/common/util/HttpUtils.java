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
 * HTTP request utility helpers.
 */
public class HttpUtils {

    /**
     * Parses the JSON request body into a map.
     */
    public static Map<String, Object> parseJsonRequest(HttpServletRequest request) throws IOException {
        return JacksonUtils.toObject(request.getReader().lines().collect(Collectors.joining()), new TypeReference<>() {
        });
    }

    /**
     * Reads a typed parameter from a parsed request map.
     */
    public static <T> T obtainParamValue(String param, Map<String, Object> requestJson, Class<T> clazz) {
        return Optional.ofNullable(requestJson.get(param))
                .map(clazz::cast)
                .orElse(null);
    }

    public static HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
