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
 * @author: Pot
 * @created: 2025/2/22 16:48
 * @description: Bean工具类
 */
public class BeanUtils {

    /**
     * BeanCopier缓存，提升性能
     */
    private static final Map<String, BeanCopier> BEAN_COPIER_CACHE = new ConcurrentHashMap<>();

    /**
     * 私有构造函数，防止实例化
     */
    private BeanUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 复制属性（浅拷贝，忽略null值）
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        org.springframework.beans.BeanUtils.copyProperties(source, target);
    }

    /**
     * 复制属性（浅拷贝，可指定忽略的属性）
     *
     * @param source           源对象
     * @param target           目标对象
     * @param ignoreProperties 忽略的属性名
     */
    public static void copyProperties(Object source, Object target, String... ignoreProperties) {
        if (source == null || target == null) {
            return;
        }
        org.springframework.beans.BeanUtils.copyProperties(source, target, ignoreProperties);
    }

    /**
     * 复制属性（忽略null值）
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyPropertiesIgnoreNull(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        org.springframework.beans.BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
    }

    /**
     * 高性能属性复制（使用CGLIB，适合大批量操作）
     *
     * @param source 源对象
     * @param target 目标对象
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
     * 转换对象类型
     *
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 目标对象
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
     * 转换对象类型（使用Supplier）
     *
     * @param source         源对象
     * @param targetSupplier 目标对象供应商
     * @param <T>            目标类型
     * @return 目标对象
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
     * 转换对象类型（支持自定义回调）
     *
     * @param source         源对象
     * @param targetSupplier 目标对象供应商
     * @param callback       自定义回调
     * @param <S>            源类型
     * @param <T>            目标类型
     * @return 目标对象
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
     * 批量转换列表
     *
     * @param sourceList  源列表
     * @param targetClass 目标类型
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 目标列表
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
     * 批量转换列表（使用Supplier）
     *
     * @param sourceList     源列表
     * @param targetSupplier 目标对象供应商
     * @param <S>            源类型
     * @param <T>            目标类型
     * @return 目标列表
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
     * 批量转换列表（支持自定义回调）
     *
     * @param sourceList     源列表
     * @param targetSupplier 目标对象供应商
     * @param callback       自定义回调
     * @param <S>            源类型
     * @param <T>            目标类型
     * @return 目标列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Supplier<T> targetSupplier, BiConsumer<S, T> callback) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> convert(source, targetSupplier, callback))
                .collect(Collectors.toList());
    }

    /**
     * 深拷贝转换对象类型(基于JSON序列化)
     *
     * <p>适用场景:
     * <ul>
     *   <li>需要深拷贝嵌套对象</li>
     *   <li>处理复杂类型转换</li>
     *   <li>避免引用共享问题</li>
     * </ul>
     *
     * <p>性能考虑: JSON序列化开销较大,不适合高频调用场景
     *
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 目标对象
     * @throws IllegalArgumentException 转换失败时抛出
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
                    e
            );
        }
    }

    /**
     * 深拷贝批量转换列表
     *
     * @param sourceList  源列表
     * @param targetClass 目标类型
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 目标列表
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
     * 混合转换策略(浅拷贝+深拷贝回调)
     *
     * <p>推荐使用场景: 大部分字段浅拷贝,少数复杂字段深拷贝
     *
     * @param source         源对象
     * @param targetSupplier 目标对象供应商
     * @param deepCopyFields 需要深拷贝的字段处理器
     * @param <S>            源类型
     * @param <T>            目标类型
     * @return 目标对象
     */
    public static <S, T> T convertHybrid(S source, Supplier<T> targetSupplier,
                                         BiConsumer<S, T> deepCopyFields) {
        if (source == null) {
            return null;
        }
        // 先执行浅拷贝
        T target = targetSupplier.get();
        copyProperties(source, target);

        // 再处理需要深拷贝的字段
        if (deepCopyFields != null) {
            deepCopyFields.accept(source, target);
        }
        return target;
    }

    /**
     * 混合转换策略批量转换列表(浅拷贝+深拷贝回调)
     *
     * <p>推荐使用场景: 批量转换时,大部分字段浅拷贝,少数复杂字段深拷贝
     *
     * @param sourceList     源列表
     * @param targetSupplier 目标对象供应商
     * @param deepCopyFields 需要深拷贝的字段处理器
     * @param <S>            源类型
     * @param <T>            目标类型
     * @return 目标列表
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
     * 获取对象中值为null的属性名
     *
     * @param source 源对象
     * @return null属性名数组
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
     * 生成缓存key
     *
     * @param sourceClass 源类型
     * @param targetClass 目标类型
     * @return 缓存key
     */
    private static String generateKey(Class<?> sourceClass, Class<?> targetClass) {
        return sourceClass.getName() + "_" + targetClass.getName();
    }

    /**
     * 清空BeanCopier缓存
     */
    public static void clearCache() {
        BEAN_COPIER_CACHE.clear();
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    public static int getCacheSize() {
        return BEAN_COPIER_CACHE.size();
    }
}