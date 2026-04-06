package com.pot.zing.framework.common.handler;

import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * Base exception handler for shared validation, business, and fallback
 * responses.
 */
@RestControllerAdvice
@Slf4j
public abstract class BaseGlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidationException(MethodArgumentNotValidException ex) {
        String message = Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage();
        return R.fail(ResultCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException ex) {
        log.warn("Business error: {}", ex.getMessage());
        return R.fail(ex.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleGeneralException(Exception ex) {
        log.error("System error: {}", ex.getMessage(), ex);
        boolean isProduction = false;
        String message = isProduction ? "系统繁忙，请稍后重试" : ex.getMessage();
        return R.fail(ResultCode.INTERNAL_ERROR, message);
    }
}
