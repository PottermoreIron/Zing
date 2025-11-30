package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * OAuth2 一键认证请求
 *
 * <p>
 * 用于第三方 OAuth2 提供商认证（Google、GitHub、Facebook、Apple、Microsoft）
 *
 * <p>
 * 认证特点：
 * <ul>
 * <li>用户不存在 → 自动创建用户（使用第三方返回的信息）</li>
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
 *   "authType": "OAUTH2",
 *   "provider": "GOOGLE",
 *   "code": "4/0AY0e-g7...",
 *   "state": "random_state",
 *   "userDomain": "MEMBER"
 * }
 * </pre>
 *
 * @author pot
 * @since 2025-11-30
 */
public record OAuth2AuthRequest(
        @NotNull(message = "认证类型不能为空") @JsonProperty("authType") AuthType authType,

        @NotNull(message = "OAuth2提供商不能为空") @JsonProperty("provider") OAuth2Provider provider,

        @NotBlank(message = "授权码不能为空") @JsonProperty("code") String code,

        @JsonProperty("state") String state,

        @NotNull(message = "用户域不能为空") @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements OneStopAuthRequest {

    /**
     * 构造函数验证
     */
    public OAuth2AuthRequest {
        if (authType != null && authType != AuthType.OAUTH2) {
            throw new IllegalArgumentException("OAuth2AuthRequest 的 authType 必须是 OAUTH2");
        }
    }

    /**
     * OAuth2 提供商枚举
     */
    public enum OAuth2Provider {
        /**
         * Google OAuth2
         */
        GOOGLE("google", "Google"),

        /**
         * GitHub OAuth2
         */
        GITHUB("github", "GitHub"),

        /**
         * Facebook OAuth2
         */
        FACEBOOK("facebook", "Facebook"),

        /**
         * Apple Sign-In
         */
        APPLE("apple", "Apple"),

        /**
         * Microsoft OAuth2
         */
        MICROSOFT("microsoft", "Microsoft");

        private final String code;
        private final String displayName;

        OAuth2Provider(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        /**
         * 从代码获取枚举
         *
         * @param code 提供商代码
         * @return OAuth2Provider 枚举
         * @throws IllegalArgumentException 如果代码不存在
         */
        public static OAuth2Provider fromCode(String code) {
            for (OAuth2Provider provider : values()) {
                if (provider.code.equalsIgnoreCase(code)) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("未知的 OAuth2 提供商: " + code);
        }

        /**
         * 获取提供商代码
         *
         * @return 提供商代码（小写）
         */
        public String getCode() {
            return code;
        }

        /**
         * 获取显示名称
         *
         * @return 显示名称
         */
        public String getDisplayName() {
            return displayName;
        }
    }
}
