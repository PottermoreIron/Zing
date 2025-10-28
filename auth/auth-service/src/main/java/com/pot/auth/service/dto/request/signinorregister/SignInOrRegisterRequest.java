package com.pot.auth.service.dto.request.signinorregister;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pot.auth.service.enums.SignInOrRegisterType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 一键登录/注册请求基类
 *
 * <p>功能说明：</p>
 * <ul>
 *   <li>未注册用户：自动注册后登录</li>
 *   <li>已注册用户：直接登录</li>
 *   <li>统一返回认证令牌和用户信息</li>
 * </ul>
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
        @JsonSubTypes.Type(value = PhoneCodeSignInOrRegisterRequest.class, name = "1"),
        @JsonSubTypes.Type(value = EmailCodeSignInOrRegisterRequest.class, name = "2")
})
public abstract class SignInOrRegisterRequest {
    @NotNull(message = "认证类型不能为空")
    protected SignInOrRegisterType type;
}

