package com.pot.auth.interfaces.exception;

import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.zing.framework.common.handler.BaseGlobalExceptionHandler;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.authorization.exception.PermissionDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps auth-service exceptions to unified API responses.
 *
 * @author pot
 * @since 2025-11-10
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<R<Void>> handleDomainException(DomainException e) {
        log.warn("[Exception] {} — {}", e.getResultCode().getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(R.fail(e.getResultCode(), e.getMessage()));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handlePermissionDenied(PermissionDeniedException e) {
        log.warn("[Exception] Access denied: {}", e.getMessage());
        return R.fail(AuthResultCode.PERMISSION_DENIED, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[Exception] Invalid parameter: {}", e.getMessage());
        return R.fail(AuthResultCode.INVALID_PARAMETER, e.getMessage());
    }
}
