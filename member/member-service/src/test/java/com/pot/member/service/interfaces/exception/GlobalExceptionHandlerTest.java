package com.pot.member.service.interfaces.exception;

import com.pot.member.service.application.exception.MemberException;
import com.pot.member.service.application.exception.MemberResultCode;
import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.model.R;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.ResponseEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Member GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Business exception uses member result code")
    void handleBusinessException_usesMemberResultCode() {
        ResponseEntity<R<?>> response = handler
                .handleBusinessException(new MemberException(MemberResultCode.MEMBER_NOT_FOUND));

        assertThat(response.getBody().getCode()).isEqualTo(MemberResultCode.MEMBER_NOT_FOUND.getCode());
        assertThat(response.getBody().getMsg()).isEqualTo(MemberResultCode.MEMBER_NOT_FOUND.getMsg());
    }

    @Test
    @DisplayName("Illegal argument exception maps to PARAM_ERROR")
    void handleIllegalArgument_returnsParamError() {
        R<Void> response = handler.handleIllegalArgument(new IllegalArgumentException("Nickname must not be blank"));

        assertThat(response.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
        assertThat(response.getMsg()).isEqualTo("Nickname must not be blank");
    }
}