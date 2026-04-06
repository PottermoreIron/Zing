package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pot.auth.application.command.OneStopAuthCommand;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;

/**
 * Base contract for authenticate-or-register requests.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "authType", visible = true)
@JsonSubTypes({
                @JsonSubTypes.Type(value = UsernamePasswordAuthRequest.class, name = "USERNAME_PASSWORD"),
                @JsonSubTypes.Type(value = PhonePasswordAuthRequest.class, name = "PHONE_PASSWORD"),
                @JsonSubTypes.Type(value = PhoneCodeAuthRequest.class, name = "PHONE_CODE"),
                @JsonSubTypes.Type(value = EmailPasswordAuthRequest.class, name = "EMAIL_PASSWORD"),
                @JsonSubTypes.Type(value = EmailCodeAuthRequest.class, name = "EMAIL_CODE"),
                @JsonSubTypes.Type(value = OAuth2AuthRequest.class, name = "OAUTH2"),
                @JsonSubTypes.Type(value = WeChatAuthRequest.class, name = "WECHAT")
})
public interface OneStopAuthRequest extends OneStopAuthCommand {

        AuthType authType();

        UserDomain userDomain();

        @Override
        default String oauth2ProviderCode() {
                if (this instanceof OAuth2AuthRequest request) {
                        return request.provider().getCode();
                }
                return null;
        }
}
