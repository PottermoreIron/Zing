package com.pot.zing.framework.common.handler;

import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseGlobalExceptionHandler")
class BaseGlobalExceptionHandlerTest {

    private final BaseGlobalExceptionHandler handler = new DefaultGlobalExceptionHandler();

    @Test
    @DisplayName("业务异常使用调用方结果码")
    void handleBusinessException_usesCustomResultCode() {
        BusinessException exception = new BusinessException(ResultCode.FORBIDDEN, "禁止访问测试");

        R<?> response = handler.handleBusinessException(exception);

        assertThat(response.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode());
        assertThat(response.getMsg()).isEqualTo("禁止访问测试");
    }

    @Test
    @DisplayName("参数校验异常合并字段错误消息")
    void handleValidationException_combinesFieldMessages() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SampleRequest(), "sampleRequest");
        bindingResult.addError(new FieldError("sampleRequest", "nickname", "昵称不能为空"));
        bindingResult.addError(new FieldError("sampleRequest", "email", "邮箱格式不正确"));
        Method method = SampleController.class.getDeclaredMethod("create", SampleRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        R<?> response = handler.handleValidationException(exception);

        assertThat(response.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
        assertThat(response.getMsg()).isEqualTo("昵称不能为空; 邮箱格式不正确");
    }

    @Test
    @DisplayName("约束异常映射为参数错误")
    void handleConstraintViolationException_returnsParamError() {
        ConstraintViolationException exception = new ConstraintViolationException(Set.of());

        R<?> response = handler.handleConstraintViolationException(exception);

        assertThat(response.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
        assertThat(response.getMsg()).isEqualTo(ResultCode.PARAM_ERROR.getMsg());
    }

    @Test
    @DisplayName("请求体不可读映射为参数错误")
    void handleHttpMessageNotReadableException_returnsParamError() {
        R<?> response = handler.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("bad body", unreadableInputMessage()));

        assertThat(response.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
        assertThat(response.getMsg()).isEqualTo("请求参数格式错误");
    }

    @Test
    @DisplayName("缺少请求参数映射为参数错误")
    void handleMissingServletRequestParameterException_returnsParamError() {
        R<?> response = handler.handleMissingServletRequestParameterException(
                new MissingServletRequestParameterException("memberId", "Long"));

        assertThat(response.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
        assertThat(response.getMsg()).isEqualTo("缺少必填参数: memberId");
    }

    private static final class SampleController {
        @SuppressWarnings("unused")
        private void create(SampleRequest request) {
        }
    }

    private static final class SampleRequest {
    }

    private HttpInputMessage unreadableInputMessage() {
        return new HttpInputMessage() {
            @Override
            public java.io.InputStream getBody() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public HttpHeaders getHeaders() {
                return HttpHeaders.EMPTY;
            }
        };
    }
}