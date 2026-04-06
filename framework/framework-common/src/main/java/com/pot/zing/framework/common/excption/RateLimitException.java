package com.pot.zing.framework.common.excption;

import com.pot.zing.framework.common.enums.ResultCode;

/**
 * Exception thrown when a rate limit is exceeded.
 */
public class RateLimitException extends BusinessException {
    public RateLimitException(ResultCode resultCode) {
        super(resultCode);
    }
}
