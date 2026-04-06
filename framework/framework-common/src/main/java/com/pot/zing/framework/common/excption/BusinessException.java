package com.pot.zing.framework.common.excption;

import com.pot.zing.framework.common.enums.ResultCode;
import lombok.Getter;

/**
 * Base exception for business failures.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public BusinessException(String message) {
        super(message);
        this.resultCode = ResultCode.INTERNAL_ERROR;
    }
}
