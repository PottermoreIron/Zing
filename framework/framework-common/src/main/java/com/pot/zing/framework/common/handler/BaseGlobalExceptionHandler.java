package com.pot.zing.framework.common.handler;

import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

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
    @ResponseStatus(HttpStatus.BAD_REQUEST)
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
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleBusinessException(BusinessException ex) {
        log.warn("Business error: {}", ex.getMessage());
        return R.fail(ex.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.PARAM_ERROR, message.isBlank() ? ResultCode.PARAM_ERROR.getMsg() : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Request body parse error: {}", ex.getMessage());
        return R.fail(ResultCode.PARAM_ERROR, "Malformed request body");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        return R.fail(ResultCode.PARAM_ERROR, "Missing required parameter: " + ex.getParameterName());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleGeneralException(Exception ex) {
        log.error("System error: {}", ex.getMessage(), ex);
        return R.fail(ResultCode.INTERNAL_ERROR, ResultCode.INTERNAL_ERROR.getMsg());
    }
}
