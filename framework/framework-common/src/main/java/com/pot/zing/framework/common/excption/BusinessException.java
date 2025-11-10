package com.pot.zing.framework.common.excption;

import com.pot.zing.framework.common.enums.ResultCode;
import lombok.Getter;

/**
 * @author: Pot
 * @created: 2025/3/16 22:37
 * @description: 业务异常类
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
