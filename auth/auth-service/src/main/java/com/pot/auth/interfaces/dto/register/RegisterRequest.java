package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pot.auth.application.command.RegisterCommand;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;

/**
 * Base contract for register requests.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "registerType", visible = true)
@JsonSubTypes({
                @JsonSubTypes.Type(value = UsernamePasswordRegisterRequest.class, name = "USERNAME_PASSWORD"),
                @JsonSubTypes.Type(value = EmailPasswordRegisterRequest.class, name = "EMAIL_PASSWORD"),
                @JsonSubTypes.Type(value = EmailCodeRegisterRequest.class, name = "EMAIL_CODE"),
                @JsonSubTypes.Type(value = PhoneCodeRegisterRequest.class, name = "PHONE_CODE"),
                @JsonSubTypes.Type(value = OAuth2RegisterRequest.class, name = "OAUTH2"),
                @JsonSubTypes.Type(value = WeChatRegisterRequest.class, name = "WECHAT")
})
public sealed interface RegisterRequest extends RegisterCommand permits
                UsernamePasswordRegisterRequest,
                EmailPasswordRegisterRequest,
                EmailCodeRegisterRequest,
                PhoneCodeRegisterRequest,
                OAuth2RegisterRequest,
                WeChatRegisterRequest {

        RegisterType registerType();

        UserDomain userDomain();

        @Override
        default String oauth2ProviderCode() {
                if (this instanceof OAuth2RegisterRequest request) {
                        return request.provider().getCode();
                }
                return null;
        }
}
