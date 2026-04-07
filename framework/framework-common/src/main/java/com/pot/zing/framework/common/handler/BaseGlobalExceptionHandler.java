package com.pot.zing.framework.common.handler;

import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Base exception handler for shared validation, business, and fallback
 * responses.
 */
@Slf4j
public abstract class BaseGlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> messages = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .toList();
        String message = messages.isEmpty()
                ? ResultCode.PARAM_ERROR.getMsg()
                : String.join("; ", messages);
        return R.fail(ResultCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException ex) {
        log.warn("Business error: {}", ex.getMessage());
        return R.fail(ex.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<?> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.PARAM_ERROR, message.isBlank() ? ResultCode.PARAM_ERROR.getMsg() : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Request body parse error: {}", ex.getMessage());
        return R.fail(ResultCode.PARAM_ERROR, "请求参数格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        return R.fail(ResultCode.PARAM_ERROR, "缺少必填参数: " + ex.getParameterName());
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleGeneralException(Exception ex) {
        log.error("System error: {}", ex.getMessage(), ex);
        boolean isProduction = false;
        String message = isProduction ? "系统繁忙，请稍后重试" : ex.getMessage();
        return R.fail(ResultCode.INTERNAL_ERROR, message);
    }
}
