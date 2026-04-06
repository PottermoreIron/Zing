package com.pot.zing.framework.starter.id.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for distributed ID generation.
 */
@Data
@ConfigurationProperties(prefix = "pot.id")
public class IdProperties {

    private boolean enabled = true;

    private String type = "leaf";

    private LeafProperties leaf = new LeafProperties();

    @Data
    public static class LeafProperties {
        private boolean segmentEnabled = true;

        private String url;
        private String username;
        private String password;
    }
}
