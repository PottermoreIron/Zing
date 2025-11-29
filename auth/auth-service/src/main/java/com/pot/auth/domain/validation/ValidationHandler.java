package com.pot.auth.domain.validation;

/**
 * 校验处理器接口
 *
 * <p>
 * 责任链模式的核心接口，用于执行各类校验逻辑
 * <p>
 * 每个校验器负责单一职责的校验，通过 order 控制执行顺序
 *
 * @param <T> 校验上下文类型
 * @author pot
 * @since 2025-11-29
 */
public interface ValidationHandler<T> {

    /**
     * 执行校验
     *
     * @param context 校验上下文
     */
    void validate(T context);

    /**
     * 获取执行顺序
     *
     * <p>
     * 数值越小，优先级越高
     * <p>
     * 建议值：
     * <ul>
     * <li>1-100: 参数校验（快速失败）</li>
     * <li>101-200: 业务规则校验</li>
     * <li>201-300: 风控校验（可能涉及外部调用）</li>
     * </ul>
     *
     * @return 执行顺序
     */
    int getOrder();

    /**
     * 是否启用此校验器
     *
     * <p>
     * 默认启用，子类可覆盖以支持动态开关
     *
     * @param context 校验上下文
     * @return true=启用，false=跳过
     */
    default boolean isEnabled(T context) {
        return true;
    }
}
