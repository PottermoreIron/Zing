package com.pot.auth.application.validation;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class ValidationChain<T> {

    private final List<ValidationHandler<T>> handlers = new ArrayList<>();

    public void addHandler(ValidationHandler<T> handler) {
        handlers.add(handler);
        handlers.sort(Comparator.comparingInt(ValidationHandler::getOrder));
    }

    public void validate(T context) {
        if (handlers.isEmpty()) {
            log.warn("[ValidationChain] No validators configured");
            return;
        }

        for (ValidationHandler<T> handler : handlers) {
            if (!handler.isEnabled(context)) {
                continue;
            }
            handler.validate(context);
        }
    }
}