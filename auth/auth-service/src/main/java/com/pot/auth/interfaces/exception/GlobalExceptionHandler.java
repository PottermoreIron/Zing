package com.pot.auth.interfaces.exception;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.exception.InvalidEmailException;
import com.pot.auth.domain.shared.exception.InvalidPhoneException;
import com.pot.auth.domain.shared.exception.WeakPasswordException;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import com.pot.zing.framework.common.handler.BaseGlobalExceptionHandler;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.authorization.exception.PermissionDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    @ExceptionHandler(JwtTokenService.TokenExpiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleTokenExpired(JwtTokenService.TokenExpiredException e) {
        log.warn("[Exception] Token expired: {}", e.getMessage());
        return R.fail(AuthResultCode.TOKEN_EXPIRED);
    }

    @ExceptionHandler(JwtTokenService.TokenInvalidException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleTokenInvalid(JwtTokenService.TokenInvalidException e) {
        log.warn("[Exception] Token invalid: {}", e.getMessage());
        return R.fail(AuthResultCode.TOKEN_INVALID);
    }

    @ExceptionHandler(VerificationCodeService.CodeSendTooFrequentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleCodeSendTooFrequent(VerificationCodeService.CodeSendTooFrequentException e) {
        log.warn("[Exception] Verification code sent too frequently: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_SEND_TOO_FREQUENT);
    }

    @ExceptionHandler(VerificationCodeService.CodeNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleCodeNotFound(VerificationCodeService.CodeNotFoundException e) {
        log.warn("[Exception] Verification code not found: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_NOT_FOUND);
    }

    @ExceptionHandler(VerificationCodeService.CodeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleCodeMismatch(VerificationCodeService.CodeMismatchException e) {
        log.warn("[Exception] Incorrect verification code: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_MISMATCH);
    }

    @ExceptionHandler(VerificationCodeService.CodeVerificationExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleCodeVerificationExceeded(VerificationCodeService.CodeVerificationExceededException e) {
        log.warn("[Exception] Verification attempt limit exceeded: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_VERIFICATION_EXCEEDED);
    }

    @ExceptionHandler(VerificationCode.InvalidVerificationCodeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleInvalidVerificationCode(VerificationCode.InvalidVerificationCodeException e) {
        log.warn("[Exception] Invalid verification code format: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_FORMAT_INVALID);
    }

    @ExceptionHandler(InvalidEmailException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleInvalidEmail(InvalidEmailException e) {
        log.warn("[Exception] Invalid email format: {}", e.getMessage());
        return R.fail(AuthResultCode.INVALID_EMAIL);
    }

    @ExceptionHandler(InvalidPhoneException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleInvalidPhone(InvalidPhoneException e) {
        log.warn("[Exception] Invalid phone format: {}", e.getMessage());
        return R.fail(AuthResultCode.INVALID_PHONE);
    }

    @ExceptionHandler(WeakPasswordException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleWeakPassword(WeakPasswordException e) {
        log.warn("[Exception] Weak password: {}", e.getMessage());
        return R.fail(AuthResultCode.WEAK_PASSWORD);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[Exception] Invalid parameter: {}", e.getMessage());
        return R.fail(AuthResultCode.INVALID_PARAMETER, e.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleDomainException(DomainException e) {
        if (e.getResultCode() != null) {
            log.warn("[Exception] Domain exception — code={}, msg={}", e.getResultCode().getCode(), e.getMessage());
            return R.fail(e.getResultCode());
        }
        log.warn("[Exception] Domain exception: {}", e.getMessage());
        return R.fail(AuthResultCode.SYSTEM_ERROR, e.getMessage());
    }

    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handlePermissionDenied(PermissionDeniedException e) {
        log.warn("[Exception] Access denied: {}", e.getMessage());
        return R.fail(AuthResultCode.PERMISSION_DENIED, e.getMessage());
    }
}
