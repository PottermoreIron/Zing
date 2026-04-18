package com.pot.zing.framework.common.handler;

import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.common.service.IResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

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

    /**
     * Maps {@link BusinessException} to the appropriate HTTP status derived from
     * its result code.
     *
     * <p>
     * The code string is checked against known HTTP status values so that
     * subclasses such as
     * {@code RateLimitException} (code "429") are returned with the correct HTTP
     * status without
     * requiring a separate handler per subtype.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<?>> handleBusinessException(BusinessException ex) {
        log.warn("Business error: {}", ex.getMessage());
        HttpStatus status = resolveHttpStatus(ex.getResultCode());
        return ResponseEntity.status(status).body(R.fail(ex.getResultCode(), ex.getMessage()));
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

    /**
     * Resolves the HTTP status from the result code string.
     * Falls back to {@code 400 Bad Request} for any unrecognized code.
     */
    private HttpStatus resolveHttpStatus(IResultCode resultCode) {
        try {
            int httpCode = Integer.parseInt(resultCode.getCode());
            HttpStatus status = HttpStatus.resolve(httpCode);
            return status != null ? status : HttpStatus.BAD_REQUEST;
        } catch (NumberFormatException ignored) {
            return HttpStatus.BAD_REQUEST;
        }
    }
}
