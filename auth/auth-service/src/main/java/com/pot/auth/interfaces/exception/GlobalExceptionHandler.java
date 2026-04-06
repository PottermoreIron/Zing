package com.pot.auth.interfaces.exception;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.exception.InvalidEmailException;
import com.pot.auth.domain.shared.exception.InvalidPhoneException;
import com.pot.auth.domain.shared.exception.WeakPasswordException;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.authorization.exception.PermissionDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.stream.Collectors;

/**
 * Maps auth-service exceptions to unified API responses.
 *
 * @author pot
 * @since 2025-11-10
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JwtTokenService.TokenExpiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleTokenExpired(JwtTokenService.TokenExpiredException e) {
        log.warn("[异常] Token过期: {}", e.getMessage());
        return R.fail(AuthResultCode.TOKEN_EXPIRED);
    }

    @ExceptionHandler(JwtTokenService.TokenInvalidException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleTokenInvalid(JwtTokenService.TokenInvalidException e) {
        log.warn("[异常] Token无效: {}", e.getMessage());
        return R.fail(AuthResultCode.TOKEN_INVALID);
    }

    @ExceptionHandler(VerificationCodeService.CodeSendTooFrequentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleCodeSendTooFrequent(VerificationCodeService.CodeSendTooFrequentException e) {
        log.warn("[异常] 验证码发送过于频繁: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_SEND_TOO_FREQUENT);
    }

    @ExceptionHandler(VerificationCodeService.CodeNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleCodeNotFound(VerificationCodeService.CodeNotFoundException e) {
        log.warn("[异常] 验证码不存在: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_NOT_FOUND);
    }

    @ExceptionHandler(VerificationCodeService.CodeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleCodeMismatch(VerificationCodeService.CodeMismatchException e) {
        log.warn("[异常] 验证码错误: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_MISMATCH);
    }

    @ExceptionHandler(VerificationCodeService.CodeVerificationExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleCodeVerificationExceeded(VerificationCodeService.CodeVerificationExceededException e) {
        log.warn("[异常] 验证次数超限: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_VERIFICATION_EXCEEDED);
    }

    @ExceptionHandler(VerificationCode.InvalidVerificationCodeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleInvalidVerificationCode(VerificationCode.InvalidVerificationCodeException e) {
        log.warn("[异常] 验证码格式错误: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_FORMAT_INVALID);
    }

    @ExceptionHandler(InvalidEmailException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleInvalidEmail(InvalidEmailException e) {
        log.warn("[异常] 邮箱格式错误: {}", e.getMessage());
        return R.fail(AuthResultCode.INVALID_EMAIL);
    }

    @ExceptionHandler(InvalidPhoneException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleInvalidPhone(InvalidPhoneException e) {
        log.warn("[异常] 手机号格式错误: {}", e.getMessage());
        return R.fail(AuthResultCode.INVALID_PHONE);
    }

    @ExceptionHandler(WeakPasswordException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleWeakPassword(WeakPasswordException e) {
        log.warn("[异常] 密码强度不足: {}", e.getMessage());
        return R.fail(AuthResultCode.WEAK_PASSWORD);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("[异常] 参数验证失败: {}", errorMessage);
        return R.fail(AuthResultCode.INVALID_PARAMETER, errorMessage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[异常] 参数错误: {}", e.getMessage());
        return R.fail(AuthResultCode.INVALID_PARAMETER, e.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleDomainException(DomainException e) {
        if (e.getResultCode() != null) {
            log.warn("[异常] 领域异常: code={}, msg={}", e.getResultCode().getCode(), e.getMessage());
            return R.fail(e.getResultCode());
        }
        log.warn("[异常] 领域异常: {}", e.getMessage());
        return R.fail(AuthResultCode.SYSTEM_ERROR, e.getMessage());
    }

    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handlePermissionDenied(PermissionDeniedException e) {
        log.warn("[异常] 权限拒绝: {}", e.getMessage());
        return R.fail(AuthResultCode.PERMISSION_DENIED, e.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.joining("; "));
        log.warn("[异常] 参数约束违反: {}", errorMessage);
        return R.fail(AuthResultCode.INVALID_PARAMETER, errorMessage);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[异常] 请求体解析失败: {}", e.getMessage());
        return R.fail(AuthResultCode.INVALID_PARAMETER, "请求参数格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.warn("[异常] 缺少请求参数: {}", e.getParameterName());
        return R.fail(AuthResultCode.INVALID_PARAMETER, "缺少必填参数: " + e.getParameterName());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("[异常] 系统异常", e);
        return R.fail(AuthResultCode.SYSTEM_ERROR);
    }
}
