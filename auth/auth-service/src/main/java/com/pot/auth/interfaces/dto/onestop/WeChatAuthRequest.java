package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 微信一键认证请求
 *
 * <p>
 * 用于微信扫码登录
 *
 * <p>
 * 认证特点：
 * <ul>
 * <li>用户不存在 → 自动创建用户（使用微信返回的昵称和头像）</li>
 * <li>用户已存在 → 直接登录</li>
 * <li>无需用户输入密码</li>
 * </ul>
 *
 * <p>
 * 请求示例：
 *
 * <pre>
 * POST /auth/api/v1/authenticate
 * {
 *   "authType": "WECHAT",
 *   "code": "071abc123",
 *   "state": "random_state",
 *   "userDomain": "MEMBER"
 * }
 * </pre>
 *
 * @author pot
 * @since 2025-11-30
 */
public record WeChatAuthRequest(
        @NotNull(message = "认证类型不能为空") @JsonProperty("authType") AuthType authType,

        @NotBlank(message = "微信授权码不能为空") @JsonProperty("code") String code,

        @JsonProperty("state") String state,

        @NotNull(message = "用户域不能为空") @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements OneStopAuthRequest {

    /**
     * 构造函数验证
     */
    public WeChatAuthRequest {
        if (authType != null && authType != AuthType.WECHAT) {
            throw new IllegalArgumentException("WeChatAuthRequest 的 authType 必须是 WECHAT");
        }
    }
}
