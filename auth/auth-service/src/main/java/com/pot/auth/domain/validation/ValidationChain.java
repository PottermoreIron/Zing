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
            log.warn("[ValidationChain] No validators configured");
            return;
        }

        log.debug("[ValidationChain] Starting validation with {} validator(s)", handlers.size());

        for (ValidationHandler<T> handler : handlers) {
            if (!handler.isEnabled(context)) {
                log.debug("[ValidationChain] Skipping disabled validator: {}", handler.getClass().getSimpleName());
                continue;
            }

            log.debug("[ValidationChain] Executing validator: {} (order={})",
                    handler.getClass().getSimpleName(), handler.getOrder());

            try {
                handler.validate(context);
            } catch (Exception e) {
                log.error("[ValidationChain] Validation failed — validator={}, error={}",
                        handler.getClass().getSimpleName(), e.getMessage());
                throw e;
            }
        }

        log.debug("[ValidationChain] All validations passed");
    }

    public int size() {
        return handlers.size();
    }

    public void clear() {
        handlers.clear();
    }
}
