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
 * Thread-safe Jackson serialization helpers.
 */
@Slf4j
public final class JacksonUtils {

    /**
     * Default date format.
     */
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Default time zone.
     */
    private static final String DEFAULT_TIMEZONE = "GMT+8";

    /**
     * Shared thread-safe ObjectMapper.
     */
    private static final ObjectMapper MAPPER;

    static {
        MAPPER = createDefaultObjectMapper();
    }

    private JacksonUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Creates the default ObjectMapper configuration.
     */
    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());

        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        SimpleDateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIMEZONE));
        mapper.setDateFormat(dateFormat);

        return mapper;
    }

    /**
     * Returns the shared ObjectMapper instance.
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * Serializes an object to JSON.
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", obj.getClass().getSimpleName(), e);
            throw new JsonSerializationException("JSON serialization failed", e);
        }
    }

    /**
     * Serializes an object to bytes.
     */
    public static byte[] toBytes(Object obj) {
        if (obj == null) {
            return new byte[0];
        }

        try {
            return MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to bytes: {}", obj.getClass().getSimpleName(), e);
            throw new JsonSerializationException("Byte serialization failed", e);
        }
    }

    /**
     * Deserializes JSON into an object.
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to {}: {}", clazz.getSimpleName(), json, e);
            throw new JsonDeserializationException("JSON deserialization failed", e);
        }
    }

    /**
     * Deserializes JSON into a complex type.
     */
    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to TypeReference: {}", json, e);
            throw new JsonDeserializationException("JSON deserialization failed", e);
        }
    }

    /**
     * Deserializes bytes into an object.
     */
    public static <T> T toObject(byte[] data, Class<T> clazz) {
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            return MAPPER.readValue(data, clazz);
        } catch (IOException e) {
            log.error("Failed to deserialize bytes to {}", clazz.getSimpleName(), e);
            throw new JsonDeserializationException("Byte deserialization failed", e);
        }
    }

    /**
     * Deserializes bytes into a complex type.
     */
    public static <T> T toObject(byte[] data, TypeReference<T> typeReference) {
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            return MAPPER.readValue(data, typeReference);
        } catch (IOException e) {
            log.error("Failed to deserialize bytes to TypeReference", e);
            throw new JsonDeserializationException("Byte deserialization failed", e);
        }
    }

    /**
     * Deserializes an input stream into an object.
     */
    public static <T> T toObject(InputStream inputStream, Class<T> clazz) {
        if (inputStream == null) {
            return null;
        }

        try {
            return MAPPER.readValue(inputStream, clazz);
        } catch (IOException e) {
            log.error("Failed to deserialize InputStream to {}", clazz.getSimpleName(), e);
            throw new JsonDeserializationException("Stream deserialization failed", e);
        }
    }

    /**
     * Parses JSON into a JsonNode tree.
     */
    public static JsonNode parseTree(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON tree: {}", json, e);
            throw new JsonDeserializationException("JSON tree parsing failed", e);
        }
    }

    /**
     * Creates a deep copy by serializing and deserializing the object.
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
            throw new JsonSerializationException("Deep copy failed", e);
        }
    }

    /**
     * Returns whether the JSON text is valid.
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
     * JSON serialization failure.
     */
    public static class JsonSerializationException extends RuntimeException {
        public JsonSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * JSON deserialization failure.
     */
    public static class JsonDeserializationException extends RuntimeException {
        public JsonDeserializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}