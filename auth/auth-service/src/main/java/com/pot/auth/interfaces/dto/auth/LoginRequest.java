package com.pot.auth.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.valueobject.UserDomain;

/**
 * 登录请求基础接口
 *
 * <p>
 * 使用Jackson多态序列化，通过loginType字段识别具体请求类型
 * <p>
 * 采用sealed interface限制所有可能的子类型，保证类型安全
 *
 * @author pot
 * @since 2025-11-18
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "loginType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = UsernamePasswordLoginRequest.class, name = "USERNAME_PASSWORD"),
        @JsonSubTypes.Type(value = EmailPasswordLoginRequest.class, name = "EMAIL_PASSWORD"),
        @JsonSubTypes.Type(value = EmailCodeLoginRequest.class, name = "EMAIL_CODE"),
        @JsonSubTypes.Type(value = PhoneCodeLoginRequest.class, name = "PHONE_CODE"),
        @JsonSubTypes.Type(value = OAuth2LoginRequest.class, name = "OAUTH2"),
        @JsonSubTypes.Type(value = WeChatLoginRequest.class, name = "WECHAT")
})
public sealed interface LoginRequest permits
        UsernamePasswordLoginRequest,
        EmailPasswordLoginRequest,
        EmailCodeLoginRequest,
        PhoneCodeLoginRequest,
        OAuth2LoginRequest,
        WeChatLoginRequest {

    /**
     * 获取登录类型
     */
    LoginType loginType();

    /**
     * 获取用户域
     */
    UserDomain userDomain();
}
