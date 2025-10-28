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
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException ex) {
        log.warn("Business error: {}", ex.getMessage());
        return R.fail(ex.getResultCode(), ex.getMessage());
    }

    /**
     * 处理所有不可知异常
     */
    @ExceptionHandler(Exception.class)
    public R<?> handleGeneralException(Exception ex) {
        log.error("System error: {}", ex.getMessage(), ex);
        // 生产环境不暴露具体错误信息
        // todo EnvUtils工具类
        boolean isProduction = false;
        String message = isProduction ? "系统繁忙，请稍后重试" : ex.getMessage();
        return R.fail(ResultCode.INTERNAL_ERROR, message);
    }
}
