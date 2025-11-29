package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidPhone;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotNull;

/**
 * 手机号密码认证请求
 *
 * <p>
 * 支持两种场景：
 * <ul>
 * <li>用户已存在 → 手机号+密码登录</li>
 * <li>用户不存在 → 手机号+验证码+密码注册</li>
 * </ul>
 *
 * @param authType         认证类型
 * @param phone            手机号
 * @param password         密码（可选，不提供时自动生成）
 * @param verificationCode 验证码（注册时必需）
 * @param userDomain       用户域
 * @author pot
 * @since 2025-11-29
 */
public record PhonePasswordAuthRequest(
        @NotNull(message = "认证类型不能为空") @JsonProperty("authType") AuthType authType,

        @ValidPhone String phone,

        String password,

        String verificationCode,

        @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements OneStopAuthRequest {
}
