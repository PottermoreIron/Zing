package com.pot.auth.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证默认值配置属性
 *
 * <p>
 * 用于配置一键注册时的默认信息
 *
 * @author pot
 * @since 2025-11-29
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.defaults")
public class AuthDefaultsProperties {

    /**
     * 默认头像URL
     */
    private String avatarUrl = "https://cdn.example.com/avatars/default.png";

    /**
     * 用户名前缀
     */
    private String usernamePrefix = "user_";

    /**
     * 密码配置
     */
    private PasswordConfig password = new PasswordConfig();

    /**
     * 密码配置
     */
    @Data
    public static class PasswordConfig {
        /**
         * 密码长度
         */
        private int length = 12;

        /**
         * 是否包含大写字母
         */
        private boolean includeUppercase = true;

        /**
         * 是否包含小写字母
         */
        private boolean includeLowercase = true;

        /**
         * 是否包含数字
         */
        private boolean includeDigits = true;

        /**
         * 是否包含特殊字符
         */
        private boolean includeSpecial = true;
    }
}
