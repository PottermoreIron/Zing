package com.pot.zing.framework.starter.ratelimit.exception;

import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.excption.BusinessException;

import java.io.Serial;

/**
 * Exception thrown when a rate limit is exceeded.
 *
 * <p>
 * Extends {@link BusinessException} with {@link ResultCode#RATE_LIMIT_EXCEEDED}
 * so that
 * the shared {@code BaseGlobalExceptionHandler} maps it to HTTP 429
 * automatically.
 */
public class RateLimitException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RateLimitException(String message) {
        super(ResultCode.RATE_LIMIT_EXCEEDED, message);
    }
}
