package com.pot.user.service.exception;

import com.pot.common.R;
import com.pot.common.enums.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * @author: Pot
 * @created: 2025/3/16 22:39
 * @description: 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 处理校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage();
        return R.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException ex) {
        ResultCode resultCode = ex.getResultCode();
        return R.fail(resultCode);
    }

    /**
     * 处理所有不可知异常
     */
    @ExceptionHandler(Exception.class)
    public R<?> handleAllException(Exception ex) {
        log.error("System error: {}, stack:{}", ex.getMessage(), ex.getStackTrace());
        return R.fail(ResultCode.INTERNAL_ERROR, ex.getMessage());
    }
}
