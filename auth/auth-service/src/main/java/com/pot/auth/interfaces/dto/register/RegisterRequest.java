package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;

/**
 * 注册请求基础接口
 *
 * <p>
 * 使用Jackson多态序列化，通过registerType字段识别具体请求类型
 * <p>
 * 采用sealed interface限制所有可能的子类型，保证类型安全
 *
 * @author pot
 * @since 2025-11-18
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
public sealed interface RegisterRequest permits
        UsernamePasswordRegisterRequest,
        EmailPasswordRegisterRequest,
        EmailCodeRegisterRequest,
        PhoneCodeRegisterRequest,
        OAuth2RegisterRequest,
        WeChatRegisterRequest {

    /**
     * 获取注册类型
     */
    RegisterType registerType();

    /**
     * 获取用户域
     */
    UserDomain userDomain();
}
