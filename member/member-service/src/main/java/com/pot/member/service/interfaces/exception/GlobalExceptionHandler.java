package com.pot.member.service.interfaces.exception;

import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.handler.BaseGlobalExceptionHandler;
import com.pot.zing.framework.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Member-specific extension of the shared global exception handler.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[MemberException] Invalid parameter: {}", ex.getMessage());
        return R.fail(ResultCode.PARAM_ERROR, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalState(IllegalStateException ex) {
        log.warn("[MemberException] Illegal state: {}", ex.getMessage());
        return R.fail(ResultCode.BAD_REQUEST, ex.getMessage());
    }
}