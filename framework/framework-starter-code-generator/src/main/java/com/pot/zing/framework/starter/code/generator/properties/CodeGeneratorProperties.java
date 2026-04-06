package com.pot.zing.framework.starter.code.generator.properties;

import com.pot.zing.framework.starter.code.generator.util.CodeGenerator;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Configuration properties for the code generator starter.
 */
@Data
@ConfigurationProperties(prefix = "pot.code.generator")
public class CodeGeneratorProperties {
    private boolean enabled = false;

    // Supports either a full URL or host, port, and database components.
    private String url;
    private String host;
    private Integer port = 3306;
    private String database;
    private String username;
    private String password;

    private String basePackage = "com.example";
    private String moduleName = "";

    // Defaults to System.getProperty("user.dir") when unset.
    private String projectPath;
    private String author = "generator";

    private String tablePrefix = "";
    private List<String> includeTables;
    private List<String> excludeTables;

    private boolean enableSwagger = false;
    private boolean enableLombok = true;
    private boolean restController = true;
    private String logicDeleteColumn = "deleted";

    /**
     * Converts starter properties into the generator utility config.
     */
    public CodeGenerator.GeneratorConfig toGeneratorConfig() {
        CodeGenerator.GeneratorConfig cfg = CodeGenerator.create();

        if (StringUtils.hasText(this.url)) {
            cfg.url(this.url);
        } else if (StringUtils.hasText(this.host) && StringUtils.hasText(this.database)) {
            cfg.mysql(this.host, this.port == null ? 3306 : this.port, this.database);
        }

        if (StringUtils.hasText(this.username) || this.password != null) {
            cfg.auth(this.username, this.password);
        }
        if (StringUtils.hasText(this.basePackage)) {
            cfg.basePackage(this.basePackage);
        }
        if (this.moduleName != null) {
            cfg.moduleName(this.moduleName);
        }
        if (StringUtils.hasText(this.projectPath)) {
            cfg.projectPath(this.projectPath);
        }
        cfg.author(this.author);
        cfg.tablePrefix(this.tablePrefix);
        if (this.includeTables != null && !this.includeTables.isEmpty()) {
            cfg.includeTables(this.includeTables.toArray(new String[0]));
        }
        if (this.excludeTables != null && !this.excludeTables.isEmpty()) {
            cfg.excludeTables(this.excludeTables.toArray(new String[0]));
        }
        cfg.enableSwagger(this.enableSwagger);
        cfg.enableLombok(this.enableLombok);
        cfg.restController(this.restController);
        cfg.logicDeleteColumn(this.logicDeleteColumn);

        return cfg;
    }
}
