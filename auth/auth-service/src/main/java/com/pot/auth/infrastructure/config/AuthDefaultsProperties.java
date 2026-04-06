package com.pot.auth.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Default values used by one-stop registration flows.
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.defaults")
public class AuthDefaultsProperties {

    private String avatarUrl = "https://cdn.example.com/avatars/default.png";

    /**
     * Legacy property name kept for the generated nickname prefix.
     */
    private String usernamePrefix = "user_";

    private PasswordConfig password = new PasswordConfig();

    /**
     * Generated password defaults.
     */
    @Data
    public static class PasswordConfig {
        private int length = 12;
        private boolean includeUppercase = true;
        private boolean includeLowercase = true;
        private boolean includeDigits = true;
        private boolean includeSpecial = true;
    }
}
