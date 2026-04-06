package com.pot.auth.application.validation;

public interface ValidationHandler<T> {

    void validate(T context);

    int getOrder();

    default boolean isEnabled(T context) {
        return true;
    }
}