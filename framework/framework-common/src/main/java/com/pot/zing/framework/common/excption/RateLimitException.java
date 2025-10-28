package com.pot.zing.framework.common.excption;

import com.pot.zing.framework.common.enums.ResultCode;

/**
 * @author: Pot
 * @created: 2025/8/16 22:31
 * @description: 限流异常
 */
public class RateLimitException extends BusinessException {
    public RateLimitException(ResultCode resultCode) {
        super(resultCode);
    }
}
