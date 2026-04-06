package com.pot.zing.framework.common.util;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.util.CollectionUtils;

import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Bean copy and conversion helpers.
 */
public class BeanUtils {

    /**
     * Cached BeanCopier instances keyed by source and target type.
     */
    private static final Map<String, BeanCopier> BEAN_COPIER_CACHE = new ConcurrentHashMap<>();

    private BeanUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Performs a shallow property copy.
     */
    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        org.springframework.beans.BeanUtils.copyProperties(source, target);
    }

    /**
     * Performs a shallow property copy while ignoring selected fields.
     */
    public static void copyProperties(Object source, Object target, String... ignoreProperties) {
        if (source == null || target == null) {
            return;
        }
        org.springframework.beans.BeanUtils.copyProperties(source, target, ignoreProperties);
    }

    /**
     * Performs a shallow copy while skipping null-valued fields.
     */
    public static void copyPropertiesIgnoreNull(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        org.springframework.beans.BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
    }

    /**
     * Performs a shallow copy using a cached CGLIB BeanCopier.
     */
    public static void copyPropertiesFast(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        String key = generateKey(source.getClass(), target.getClass());
        BeanCopier copier = BEAN_COPIER_CACHE.computeIfAbsent(key,
                k -> BeanCopier.create(source.getClass(), target.getClass(), false));
        copier.copy(source, target, null);
    }

    /**
     * Converts an object to the target type via shallow copy.
     */
    public static <T> T convert(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert bean", e);
        }
    }

    /**
     * Converts an object using a supplied target instance.
     */
    public static <T> T convert(Object source, Supplier<T> targetSupplier) {
        if (source == null) {
            return null;
        }
        T target = targetSupplier.get();
        copyProperties(source, target);
        return target;
    }

    /**
     * Converts an object and applies a custom callback to the result.
     */
    public static <S, T> T convert(S source, Supplier<T> targetSupplier, BiConsumer<S, T> callback) {
        if (source == null) {
            return null;
        }
        T target = targetSupplier.get();
        copyProperties(source, target);
        if (callback != null) {
            callback.accept(source, target);
        }
        return target;
    }

    /**
     * Converts a list using reflective target construction.
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass) {
        if (org.springframework.util.CollectionUtils.isEmpty(sourceList)) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> convert(source, targetClass))
                .collect(Collectors.toList());
    }

    /**
     * Converts a list using a supplied target instance factory.
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Supplier<T> targetSupplier) {
        if (org.springframework.util.CollectionUtils.isEmpty(sourceList)) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> convert(source, targetSupplier))
                .collect(Collectors.toList());
    }

    /**
     * Converts a list and applies a custom callback per item.
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Supplier<T> targetSupplier,
            BiConsumer<S, T> callback) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> convert(source, targetSupplier, callback))
                .collect(Collectors.toList());
    }

    /**
     * Converts an object with JSON-based deep copy semantics.
     */
    public static <T> T convertDeep(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            return JacksonUtils.deepCopy(source, targetClass);
        } catch (JacksonUtils.JsonSerializationException | JacksonUtils.JsonDeserializationException e) {
            throw new IllegalArgumentException(
                    String.format("Failed to deep convert from %s to %s",
                            source.getClass().getSimpleName(),
                            targetClass.getSimpleName()),
                    e);
        }
    }

    /**
     * Converts a list with JSON-based deep copy semantics.
     */
    public static <S, T> List<T> convertListDeep(List<S> sourceList, Class<T> targetClass) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> convertDeep(source, targetClass))
                .collect(Collectors.toList());
    }

    /**
     * Combines shallow copy with a custom deep-copy callback.
     */
    public static <S, T> T convertHybrid(S source, Supplier<T> targetSupplier,
            BiConsumer<S, T> deepCopyFields) {
        if (source == null) {
            return null;
        }
        T target = targetSupplier.get();
        copyProperties(source, target);

        if (deepCopyFields != null) {
            deepCopyFields.accept(source, target);
        }
        return target;
    }

    /**
     * Converts a list with shallow copy and a deep-copy callback.
     */
    public static <S, T> List<T> convertListHybrid(List<S> sourceList,
            Supplier<T> targetSupplier,
            BiConsumer<S, T> deepCopyFields) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> convertHybrid(source, targetSupplier, deepCopyFields))
                .collect(Collectors.toList());
    }

    /**
     * Returns property names whose values are null.
     */
    private static String[] getNullPropertyNames(Object source) {
        BeanWrapper wrapper = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = wrapper.getPropertyDescriptors();
        Set<String> nullProperties = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            Object value = wrapper.getPropertyValue(pd.getName());
            if (value == null) {
                nullProperties.add(pd.getName());
            }
        }
        return nullProperties.toArray(new String[0]);
    }

    /**
     * Builds the BeanCopier cache key.
     */
    private static String generateKey(Class<?> sourceClass, Class<?> targetClass) {
        return sourceClass.getName() + "_" + targetClass.getName();
    }

    /**
     * Clears the BeanCopier cache.
     */
    public static void clearCache() {
        BEAN_COPIER_CACHE.clear();
    }

    /**
     * Returns the BeanCopier cache size.
     */
    public static int getCacheSize() {
        return BEAN_COPIER_CACHE.size();
    }
}