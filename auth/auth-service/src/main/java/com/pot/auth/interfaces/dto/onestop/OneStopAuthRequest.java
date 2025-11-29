package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;

/**
 * 一键认证请求基础接口
 *
 * <p>
 * 使用Jackson多态序列化，通过authType字段识别具体请求类型
 *
 * <p>
 * <strong>支持的认证类型：</strong>
 * <ul>
 * <li>USERNAME_PASSWORD - 用户名密码</li>
 * <li>PHONE_PASSWORD - 手机号密码</li>
 * <li>PHONE_CODE - 手机号验证码</li>
 * <li>EMAIL_PASSWORD - 邮箱密码</li>
 * <li>EMAIL_CODE - 邮箱验证码</li>
 * <li>OAUTH2 - OAuth2</li>
 * <li>WECHAT - 微信</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-29
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
public sealed interface OneStopAuthRequest permits
        UsernamePasswordAuthRequest,
        PhonePasswordAuthRequest,
        PhoneCodeAuthRequest,
        EmailPasswordAuthRequest,
        EmailCodeAuthRequest,
        OAuth2AuthRequest,
        WeChatAuthRequest {

    /**
     * 获取认证类型
     */
    AuthType authType();

    /**
     * 获取用户域
     */
    UserDomain userDomain();
}
