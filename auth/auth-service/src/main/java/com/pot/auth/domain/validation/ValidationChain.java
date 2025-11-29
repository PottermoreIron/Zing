package com.pot.auth.domain.validation;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 校验链
 *
 * <p>
 * 责任链模式的编排器，按顺序执行所有已注册的校验器
 * <p>
 * 采用快速失败策略：任一校验失败立即抛出异常
 *
 * <p>
 * 使用示例：
 *
 * <pre>{@code
 * ValidationChain<AuthenticationContext> chain = new ValidationChain<>();
 * chain.addHandler(new ParameterValidationHandler());
 * chain.addHandler(new RiskControlValidationHandler());
 * chain.validate(context); // 按顺序执行所有校验
 * }</pre>
 *
 * @param <T> 校验上下文类型
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
public class ValidationChain<T> {

    private final List<ValidationHandler<T>> handlers = new ArrayList<>();

    /**
     * 添加校验处理器
     *
     * @param handler 校验处理器
     */
    public void addHandler(ValidationHandler<T> handler) {
        handlers.add(handler);
        // 添加后立即排序，保证顺序正确
        handlers.sort(Comparator.comparingInt(ValidationHandler::getOrder));
    }

    /**
     * 批量添加校验处理器
     *
     * @param handlerList 校验处理器列表
     */
    public void addHandlers(List<ValidationHandler<T>> handlerList) {
        handlers.addAll(handlerList);
        handlers.sort(Comparator.comparingInt(ValidationHandler::getOrder));
    }

    /**
     * 执行校验链
     *
     * <p>
     * 按照 order 顺序依次执行所有已启用的校验器
     * <p>
     * 快速失败：任一校验失败立即抛出异常
     *
     * @param context 校验上下文
     */
    public void validate(T context) {
        if (handlers.isEmpty()) {
            log.warn("[校验链] 未配置任何校验器");
            return;
        }

        log.debug("[校验链] 开始执行校验，共 {} 个校验器", handlers.size());

        for (ValidationHandler<T> handler : handlers) {
            if (!handler.isEnabled(context)) {
                log.debug("[校验链] 跳过已禁用的校验器: {}", handler.getClass().getSimpleName());
                continue;
            }

            log.debug("[校验链] 执行校验器: {} (order={})",
                    handler.getClass().getSimpleName(), handler.getOrder());

            try {
                handler.validate(context);
            } catch (Exception e) {
                log.error("[校验链] 校验失败: validator={}, error={}",
                        handler.getClass().getSimpleName(), e.getMessage());
                throw e;
            }
        }

        log.debug("[校验链] 所有校验通过");
    }

    /**
     * 获取已注册的校验器数量
     */
    public int size() {
        return handlers.size();
    }

    /**
     * 清空所有校验器
     */
    public void clear() {
        handlers.clear();
    }
}
