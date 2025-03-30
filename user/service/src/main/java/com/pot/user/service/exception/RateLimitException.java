package com.pot.user.service.exception;

import com.pot.common.enums.ResultCode;

/**
 * @author: Pot
 * @created: 2025/3/30 16:35
 * @description: 限流异常
 */
public class RateLimitException extends BusinessException {
    public RateLimitException(ResultCode resultCode) {
        super(resultCode);
    }
}