package com.pot.auth.service.dto.request.login;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pot.auth.service.enums.LoginType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/9/8 23:19
 * @description: 登录请求基类
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
        @JsonSubTypes.Type(value = UserNamePasswordLoginRequest.class, name = "1"),
        @JsonSubTypes.Type(value = PhonePasswordLoginRequest.class, name = "2"),
        @JsonSubTypes.Type(value = EmailPasswordLoginRequest.class, name = "3"),
        @JsonSubTypes.Type(value = PhoneCodeLoginRequest.class, name = "4"),
        @JsonSubTypes.Type(value = EmailCodeLoginRequest.class, name = "5"),
        @JsonSubTypes.Type(value = OAuth2LoginRequest.class, name = "6"),
        @JsonSubTypes.Type(value = OAuth2LoginRequest.class, name = "7"),
        @JsonSubTypes.Type(value = OAuth2LoginRequest.class, name = "8"),
        @JsonSubTypes.Type(value = OAuth2LoginRequest.class, name = "9"),
        @JsonSubTypes.Type(value = OAuth2LoginRequest.class, name = "10")
})
public abstract class LoginRequest {
    @NotNull(message = "登录类型不能为空")
    protected LoginType type;
}
