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
    @DisplayName("Permission denied exception maps to 403 and PERMISSION_DENIED")
    void handlePermissionDenied_mapsToForbidden() throws NoSuchMethodException {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        R<Void> response = handler.handlePermissionDenied(new PermissionDeniedException("Insufficient permissions"));
        Method method = GlobalExceptionHandler.class.getMethod(
                "handlePermissionDenied", PermissionDeniedException.class);
        ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);

        assertThat(response.getCode()).isEqualTo(AuthResultCode.PERMISSION_DENIED.getCode());
        assertThat(response.getMsg()).isEqualTo("Insufficient permissions");
        assertThat(responseStatus.value()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}