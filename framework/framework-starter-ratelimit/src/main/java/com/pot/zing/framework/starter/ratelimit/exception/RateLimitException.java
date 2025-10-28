package com.pot.zing.framework.starter.ratelimit.exception;

import java.io.Serial;

/**
 * @author: Pot
 * @created: 2025/10/18 22:08
 * @description: 自定义限流异常
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
