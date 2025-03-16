package com.pot.user.service.exception;

import com.pot.common.enums.ResultCode;
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
}
