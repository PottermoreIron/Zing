package com.pot.auth.domain.validation;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates validation handlers in order.
 *
 * @param <T> validation context type
 */
@Slf4j
public class ValidationChain<T> {

    private final List<ValidationHandler<T>> handlers = new ArrayList<>();

    public void addHandler(ValidationHandler<T> handler) {
        handlers.add(handler);
        handlers.sort(Comparator.comparingInt(ValidationHandler::getOrder));
    }

    public void addHandlers(List<ValidationHandler<T>> handlerList) {
        handlers.addAll(handlerList);
        handlers.sort(Comparator.comparingInt(ValidationHandler::getOrder));
    }

    /**
     * Runs all enabled handlers in order.
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

    public int size() {
        return handlers.size();
    }

    public void clear() {
        handlers.clear();
    }
}
