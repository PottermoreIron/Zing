package com.pot.auth.interfaces.exception;

import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.authorization.exception.PermissionDeniedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("权限拒绝异常映射为 403 和 PERMISSION_DENIED")
    void handlePermissionDenied_mapsToForbidden() throws NoSuchMethodException {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        R<Void> response = handler.handlePermissionDenied(new PermissionDeniedException("权限不足"));
        Method method = GlobalExceptionHandler.class.getMethod(
                "handlePermissionDenied", PermissionDeniedException.class);
        ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);

        assertThat(response.getCode()).isEqualTo(AuthResultCode.PERMISSION_DENIED.getCode());
        assertThat(response.getMsg()).isEqualTo("权限不足");
        assertThat(responseStatus.value()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}