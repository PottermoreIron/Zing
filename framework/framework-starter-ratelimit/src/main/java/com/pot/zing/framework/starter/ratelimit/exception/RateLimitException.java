package com.pot.zing.framework.starter.ratelimit.exception;

import java.io.Serial;

/**
 * Exception thrown when a rate limit is exceeded.
 */
public class RateLimitException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
