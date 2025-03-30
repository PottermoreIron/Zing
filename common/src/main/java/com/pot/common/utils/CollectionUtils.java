package com.pot.common.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author: Pot
 * @created: 2025/2/22 16:46
 * @description: 集合工具类 refer: <a href="https://github.com/YunaiV/ruoyi-vue-pro">...</a>
 */
public class CollectionUtils {

    // 空集合快速判断
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    // 通用转换方法（最终实现）
    private static <T, K, V, M extends Map<K, V>> M convertMap(
            Collection<T> source,
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction,
            Supplier<M> mapFactory) {

        if (isEmpty(source)) {
            return mapFactory.get();
        }

        return source.stream().collect(Collectors.toMap(
                keyMapper,
                valueMapper,
                mergeFunction,
                mapFactory
        ));
    }

    // 基础转换（仅Key提取）
    public static <T, K> Map<K, T> toMap(Collection<T> source, Function<T, K> keyMapper) {
        return toMap(source, keyMapper, () -> new HashMap<>());
    }

    public static <T, K, M extends Map<K, T>> M toMap(
            Collection<T> source,
            Function<T, K> keyMapper,
            Supplier<M> mapFactory) {

        return convertMap(
                source,
                keyMapper,
                Function.identity(),
                (existing, replacement) -> existing,
                mapFactory
        );
    }

    // 完整参数转换
    public static <T, K, V> Map<K, V> toMap(
            Collection<T> source,
            Function<T, K> keyMapper,
            Function<T, V> valueMapper) {

        return toMap(source, keyMapper, valueMapper, HashMap::new);
    }

    public static <T, K, V, M extends Map<K, V>> M toMap(
            Collection<T> source,
            Function<T, K> keyMapper,
            Function<T, V> valueMapper,
            Supplier<M> mapFactory) {

        return convertMap(
                source,
                keyMapper,
                valueMapper,
                (existing, replacement) -> existing,
                mapFactory
        );
    }

    // 带合并策略的转换
    public static <T, K, V> Map<K, V> toMapWithMerge(
            Collection<T> source,
            Function<T, K> keyMapper,
            Function<T, V> valueMapper,
            BinaryOperator<V> mergeFunction) {

        return toMapWithMerge(source, keyMapper, valueMapper, mergeFunction, HashMap::new);
    }

    public static <T, K, V, M extends Map<K, V>> M toMapWithMerge(
            Collection<T> source,
            Function<T, K> keyMapper,
            Function<T, V> valueMapper,
            BinaryOperator<V> mergeFunction,
            Supplier<M> mapFactory) {

        return convertMap(
                source,
                keyMapper,
                valueMapper,
                mergeFunction,
                mapFactory
        );
    }
}
