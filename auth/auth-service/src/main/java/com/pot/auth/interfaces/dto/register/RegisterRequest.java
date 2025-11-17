package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pot.auth.domain.registration.enums.RegistrationType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import jakarta.validation.constraints.NotNull;

/**
 * @author: Pot
 * @created: 2025/11/17 23:17
 * @description: 注册请求接口类
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "registrationType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = UsernamePasswordRegisterRequest.class, name = "USERNAME_PASSWORD"),
        @JsonSubTypes.Type(value = PhonePasswordRegisterRequest.class, name = "PHONE_PASSWORD"),
        @JsonSubTypes.Type(value = EmailPasswordRegisterRequest.class, name = "EMAIL_PASSWORD"),
        @JsonSubTypes.Type(value = PhoneCodeRegisterRequest.class, name = "PHONE_CODE"),
        @JsonSubTypes.Type(value = EmailCodeRegisterRequest.class, name = "EMAIL_CODE")
})
public sealed interface RegisterRequest permits
        UsernamePasswordRegisterRequest,
        PhonePasswordRegisterRequest,
        EmailPasswordRegisterRequest,
        PhoneCodeRegisterRequest,
        EmailCodeRegisterRequest {

    /**
     * 注册类型
     */
    @NotNull(message = "注册类型不能为空")
    RegistrationType registrationType();

    /**
     * 用户域
     */
    @NotNull(message = "用户域不能为空")
    UserDomain userDomain();
}
