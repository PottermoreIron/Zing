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
 * 全局异常处理器
 *
 * <p>
 * 使用Auth服务专属的错误码（AuthResultCode）
 *
 * @author pot
 * @since 2025-11-10
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理Token过期异常
     */
    @ExceptionHandler(JwtTokenService.TokenExpiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleTokenExpired(JwtTokenService.TokenExpiredException e) {
        log.warn("[异常] Token过期: {}", e.getMessage());
        return R.fail(AuthResultCode.TOKEN_EXPIRED);
    }

    /**
     * 处理Token无效异常
     */
    @ExceptionHandler(JwtTokenService.TokenInvalidException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleTokenInvalid(JwtTokenService.TokenInvalidException e) {
        log.warn("[异常] Token无效: {}", e.getMessage());
        return R.fail(AuthResultCode.TOKEN_INVALID);
    }

    /**
     * 处理验证码发送过于频繁异常
     */
    @ExceptionHandler(VerificationCodeService.CodeSendTooFrequentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleCodeSendTooFrequent(VerificationCodeService.CodeSendTooFrequentException e) {
        log.warn("[异常] 验证码发送过于频繁: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_SEND_TOO_FREQUENT);
    }

    /**
     * 处理验证码不存在异常
     */
    @ExceptionHandler(VerificationCodeService.CodeNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleCodeNotFound(VerificationCodeService.CodeNotFoundException e) {
        log.warn("[异常] 验证码不存在: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_NOT_FOUND);
    }

    /**
     * 处理验证码错误异常
     */
    @ExceptionHandler(VerificationCodeService.CodeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleCodeMismatch(VerificationCodeService.CodeMismatchException e) {
        log.warn("[异常] 验证码错误: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_MISMATCH);
    }

    /**
     * 处理验证次数超限异常
     */
    @ExceptionHandler(VerificationCodeService.CodeVerificationExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleCodeVerificationExceeded(VerificationCodeService.CodeVerificationExceededException e) {
        log.warn("[异常] 验证次数超限: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_VERIFICATION_EXCEEDED);
    }

    /**
     * 处理验证码格式错误异常
     */
    @ExceptionHandler(VerificationCode.InvalidVerificationCodeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleInvalidVerificationCode(VerificationCode.InvalidVerificationCodeException e) {
        log.warn("[异常] 验证码格式错误: {}", e.getMessage());
        return R.fail(AuthResultCode.CODE_FORMAT_INVALID);
    }

    /**
     * 处理邮箱格式错误异常
     */
    @ExceptionHandler(InvalidEmailException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleInvalidEmail(InvalidEmailException e) {
        log.warn("[异常] 邮箱格式错误: {}", e.getMessage());
        return R.fail(AuthResultCode.INVALID_EMAIL);
    }

    /**
     * 处理手机号格式错误异常
     */
    @ExceptionHandler(InvalidPhoneException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleInvalidPhone(InvalidPhoneException e) {
        log.warn("[异常] 手机号格式错误: {}", e.getMessage());
        return R.fail(AuthResultCode.INVALID_PHONE);
    }

    /**
     * 处理密码强度不足异常
     */
    @ExceptionHandler(WeakPasswordException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleWeakPassword(WeakPasswordException e) {
        log.warn("[异常] 密码强度不足: {}", e.getMessage());
        return R.fail(AuthResultCode.WEAK_PASSWORD);
    }

    /**
     * 处理Bean Validation验证失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidationException(MethodArgumentNotValidException e) {
        // 收集所有字段错误信息
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("[异常] 参数验证失败: {}", errorMessage);
        return R.fail(AuthResultCode.INVALID_PARAMETER, errorMessage);
    }

    /**
     * 处理参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[异常] 参数错误: {}", e.getMessage());
        return R.fail(AuthResultCode.INVALID_PARAMETER, e.getMessage());
    }

    /**
     * 处理通用领域异常
     *
     * <p>
     * 若异常携带 {@link AuthResultCode}，优先使用其 code 和 msg；
     * 否则降级使用 {@code SYSTEM_ERROR}。
     */
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

    /**
     * 处理@RequestParam、@PathVariable等方法参数上的Constraint违反异常
     *
     * <p>
     * 由@Validated注解在Controller类上触发，当@ValidEmail/@ValidPhone等校验失败时
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.joining("; "));
        log.warn("[异常] 参数约束违反: {}", errorMessage);
        return R.fail(AuthResultCode.INVALID_PARAMETER, errorMessage);
    }

    /**
     * 处理请求体反序列化失败（如无效的JSON格式或未知的枚举值）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[异常] 请求体解析失败: {}", e.getMessage());
        return R.fail(AuthResultCode.INVALID_PARAMETER, "请求参数格式错误");
    }

    /**
     * 处理缺少必填请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.warn("[异常] 缺少请求参数: {}", e.getParameterName());
        return R.fail(AuthResultCode.INVALID_PARAMETER, "缺少必填参数: " + e.getParameterName());
    }

    /**
     * 处理未知异常（最后一道防线）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("[异常] 系统异常", e);
        return R.fail(AuthResultCode.SYSTEM_ERROR);
    }
}
