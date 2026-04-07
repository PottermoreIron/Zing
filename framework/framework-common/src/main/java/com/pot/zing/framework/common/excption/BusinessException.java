package com.pot.zing.framework.common.excption;

import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.service.IResultCode;
import lombok.Getter;

/**
 * Base exception for business failures.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final IResultCode resultCode;

    public BusinessException(IResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }

    public BusinessException(IResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public BusinessException(String message) {
        super(message);
        this.resultCode = ResultCode.INTERNAL_ERROR;
    }
}
