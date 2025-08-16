package com.pot.common.handler;

import com.pot.common.R;
import com.pot.common.enums.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * @author: Pot
 * @created: 2025/8/16 22:52
 * @description: 全局异常处理器抽象类
 */
@RestControllerAdvice
@Slf4j
public abstract class BaseGlobalExceptionHandler {

    /**
     * 处理校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidationException(MethodArgumentNotValidException ex) {
        String message = Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage();
        return R.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理所有不可知异常
     */
    @ExceptionHandler(Exception.class)
    public R<?> handleGeneralException(Exception ex) {
        log.error("System error: {}", ex.getMessage(), ex);
        return R.fail(ResultCode.INTERNAL_ERROR, ex.getMessage());
    }
}
