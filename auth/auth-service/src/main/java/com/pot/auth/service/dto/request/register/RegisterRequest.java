package com.pot.auth.service.dto.request.register;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pot.auth.service.enums.RegisterType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/9/3 23:49
 * @description: 注册请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserNamePasswordRegisterRequest.class, name = "1"),
        @JsonSubTypes.Type(value = PhonePasswordRegisterRequest.class, name = "2"),
        @JsonSubTypes.Type(value = EmailPasswordRegisterRequest.class, name = "3"),
        @JsonSubTypes.Type(value = PhoneCodeRegisterRequest.class, name = "4"),
        @JsonSubTypes.Type(value = EmailCodeRegisterRequest.class, name = "5")
})
public abstract class RegisterRequest {
    @NotNull(message = "注册类型不能为空")
    protected RegisterType type;
}
