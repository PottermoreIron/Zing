package com.pot.user.service.exception;

import com.pot.common.R;
import com.pot.common.enums.ResultCode;
import com.pot.common.exception.BusinessException;
import com.pot.common.handler.BaseGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author: Pot
 * @created: 2025/3/16 22:39
 * @description: 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException ex) {
        ResultCode resultCode = ex.getResultCode();
        return R.fail(resultCode);
    }
}
