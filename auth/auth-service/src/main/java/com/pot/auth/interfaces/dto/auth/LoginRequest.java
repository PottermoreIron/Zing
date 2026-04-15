package com.pot.auth.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.valueobject.UserDomain;

/**
 * Base contract for login requests.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "loginType", visible = true)
@JsonSubTypes({
                @JsonSubTypes.Type(value = UsernamePasswordLoginRequest.class, name = "USERNAME_PASSWORD"),
                @JsonSubTypes.Type(value = EmailPasswordLoginRequest.class, name = "EMAIL_PASSWORD"),
                @JsonSubTypes.Type(value = EmailCodeLoginRequest.class, name = "EMAIL_CODE"),
                @JsonSubTypes.Type(value = PhoneCodeLoginRequest.class, name = "PHONE_CODE"),
                @JsonSubTypes.Type(value = PhonePasswordLoginRequest.class, name = "PHONE_PASSWORD")
})
public sealed interface LoginRequest permits
                UsernamePasswordLoginRequest,
                EmailPasswordLoginRequest,
                EmailCodeLoginRequest,
                PhoneCodeLoginRequest,
                PhonePasswordLoginRequest {

        LoginType loginType();

        UserDomain userDomain();

        default String nickname() {
                return null;
        }

        default String email() {
                return null;
        }

        default String phone() {
                return null;
        }

        default String password() {
                return null;
        }

        default String verificationCode() {
                return null;
        }
}
