package com.pot.zing.framework.starter.id.exception;

/**
 * Raised when distributed ID generation fails.
 */
public class IdGenerationException extends RuntimeException {

    public IdGenerationException(String message) {
        super(message);
    }

    public IdGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
