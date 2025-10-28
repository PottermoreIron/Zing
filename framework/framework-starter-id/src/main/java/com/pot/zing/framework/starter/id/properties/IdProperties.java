package com.pot.zing.framework.starter.id.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: Pot
 * @created: 2025/10/18 23:24
 * @description: 自定义ID生成属性
 */
@Data
@ConfigurationProperties(prefix = "pot.id")
public class IdProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * ID生成器类型
     */
    private String type = "leaf";

    /**
     * Leaf配置
     */
    private LeafProperties leaf = new LeafProperties();

    @Data
    public static class LeafProperties {
        /**
         * 是否启用号段模式
         */
        private boolean segmentEnabled = true;

        /**
         * 数据库配置(如果不使用默认数据源)
         */
        private String url;
        private String username;
        private String password;
    }
}
