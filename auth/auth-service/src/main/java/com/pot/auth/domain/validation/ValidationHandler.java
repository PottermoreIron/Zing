package com.pot.auth.domain.validation;

/**
 * Validation step used by a validation chain.
 *
 * @param <T> validation context type
 */
public interface ValidationHandler<T> {

    void validate(T context);

    /**
     * Returns the execution order for this step.
     */
    int getOrder();

    /**
     * Indicates whether this step should run for the given context.
     */
    default boolean isEnabled(T context) {
        return true;
    }
}
