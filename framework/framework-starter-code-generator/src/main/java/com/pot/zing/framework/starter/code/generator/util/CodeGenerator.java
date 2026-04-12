package com.pot.zing.framework.starter.code.generator.util;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Fluent wrapper around the MyBatis-Plus code generator.
 *
 * @author Pot
 * @since 2025-02-23
 */
public final class CodeGenerator {

    private CodeGenerator() {
    }

    /**
     * Creates a new generator config.
     */
    public static GeneratorConfig create() {
        return new GeneratorConfig();
    }

    /**
     * Generates code with default local MySQL settings.
     */
    public static void quickGenerate(String database, String basePackage) {
        quickGenerate("localhost", database, "root", "", basePackage);
    }

    /**
     * Generates code with explicit MySQL connection settings.
     */
    public static void quickGenerate(String host, String database, String username, String password,
            String basePackage) {
        create()
                .mysql(host, 3306, database)
                .auth(username, password)
                .basePackage(basePackage)
                .generate();
    }

    /**
     * Executes code generation for the supplied config.
     */
    static void execute(GeneratorConfig config) {
        validateConfig(config);

        String outputDir = buildOutputDir(config);
        String xmlOutputDir = buildXmlOutputDir(config);

        FastAutoGenerator.create(config.url(), config.username(), config.password())
                .globalConfig(builder -> {
                    builder.author(config.author())
                            .outputDir(outputDir)
                            .commentDate("yyyy-MM-dd HH:mm:ss")
                            .disableOpenDir();

                    if (config.enableSwagger()) {
                        builder.enableSwagger();
                    }
                })
                .packageConfig(builder -> {
                    String parentPackage = StringUtils.hasText(config.moduleName())
                            ? config.basePackage() + "." + config.moduleName()
                            : config.basePackage();

                    builder.parent(parentPackage)
                            .entity("entity")
                            .mapper("mapper")
                            .service("service")
                            .serviceImpl("service.impl")
                            .controller("controller")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, xmlOutputDir));
                })
                .dataSourceConfig(builder -> {
                    builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                        return switch (metaInfo.getJdbcType()) {
                            case TINYINT -> DbColumnType.INTEGER;
                            case BIT -> DbColumnType.BOOLEAN;
                            default -> typeRegistry.getColumnType(metaInfo);
                        };
                    });
                })
                .strategyConfig(builder -> {
                    configureTableStrategy(builder, config);
                    configureEntityStrategy(builder, config);
                    configureControllerStrategy(builder, config);
                    configureServiceStrategy(builder);
                    configureMapperStrategy(builder);
                })
                .templateEngine(new VelocityTemplateEngine())
                .execute();

        System.out.println("✅ 代码生成完成");
        System.out.println("📁 Java输出目录: " + outputDir);
        System.out.println("📁 XML输出目录: " + xmlOutputDir);
    }

    /**
     * Applies table-level generation settings.
     */
    private static void configureTableStrategy(com.baomidou.mybatisplus.generator.config.StrategyConfig.Builder builder,
            GeneratorConfig config) {
        if (StringUtils.hasText(config.tablePrefix())) {
            builder.addTablePrefix(config.tablePrefix());
        }

        if (!CollectionUtils.isEmpty(config.includeTable())) {
            builder.addInclude(config.includeTable());
        }

        if (!CollectionUtils.isEmpty(config.excludeTable())) {
            builder.addExclude(config.excludeTable());
        }
    }

    /**
     * Applies entity generation settings.
     */
    private static void configureEntityStrategy(
            com.baomidou.mybatisplus.generator.config.StrategyConfig.Builder builder, GeneratorConfig config) {
        var entityBuilder = builder.entityBuilder()
                .enableTableFieldAnnotation()
                .enableRemoveIsPrefix()
                .idType(config.idType())
                .formatFileName("%s");

        if (config.enableLombok()) {
            entityBuilder.enableLombok();
        }

        if (StringUtils.hasText(config.logicDeleteColumn())) {
            entityBuilder.logicDeleteColumnName(config.logicDeleteColumn());
        }

        if (!CollectionUtils.isEmpty(config.tableFills())) {
            config.tableFills().forEach(entityBuilder::addTableFills);
        }
    }

    /**
     * Applies controller generation settings.
     */
    private static void configureControllerStrategy(
            com.baomidou.mybatisplus.generator.config.StrategyConfig.Builder builder, GeneratorConfig config) {
        var controllerBuilder = builder.controllerBuilder()
                .formatFileName("%sController");

        if (config.restController()) {
            controllerBuilder.enableRestStyle();
        }
    }

    /**
     * Applies service generation settings.
     */
    private static void configureServiceStrategy(
            com.baomidou.mybatisplus.generator.config.StrategyConfig.Builder builder) {
        builder.serviceBuilder()
                .formatServiceFileName("%sService")
                .formatServiceImplFileName("%sServiceImpl");
    }

    /**
     * Applies mapper generation settings.
     */
    private static void configureMapperStrategy(
            com.baomidou.mybatisplus.generator.config.StrategyConfig.Builder builder) {
        builder.mapperBuilder()
                .enableBaseResultMap()
                .enableBaseColumnList()
                .formatMapperFileName("%sMapper")
                .formatXmlFileName("%sMapper");
    }

    /**
     * Builds the Java output directory.
     */
    private static String buildOutputDir(GeneratorConfig config) {
        String basePath = config.projectPath();
        if (StringUtils.hasText(config.moduleName())) {
            basePath = Paths.get(basePath, config.moduleName()).toString();
        }
        return Paths.get(basePath, "src", "main", "java").toString();
    }

    /**
     * Builds the XML output directory.
     */
    private static String buildXmlOutputDir(GeneratorConfig config) {
        String basePath = config.projectPath();
        if (StringUtils.hasText(config.moduleName())) {
            basePath = Paths.get(basePath, config.moduleName()).toString();
        }
        return Paths.get(basePath, "src", "main", "resources", "mapper").toString();
    }

    /**
     * Validates the supplied generator config.
     */
    private static void validateConfig(GeneratorConfig config) {
        if (!StringUtils.hasText(config.url())) {
            throw new IllegalArgumentException("❌ Database connection URL must not be blank");
        }
        if (!StringUtils.hasText(config.username())) {
            throw new IllegalArgumentException("❌ Database username must not be blank");
        }
        if (config.password() == null) {
            throw new IllegalArgumentException("❌ Database password must not be null");
        }
        if (!StringUtils.hasText(config.basePackage())) {
            throw new IllegalArgumentException("❌ Base package name must not be blank");
        }
    }

    /**
     * Fluent configuration for code generation.
     */
    @Data
    @Accessors(chain = true, fluent = true)
    public static class GeneratorConfig {

        /**
         * Database connection URL.
         */
        private String url;

        /**
         * Database username.
         */
        private String username;

        /**
         * Database password.
         */
        private String password;

        /**
         * Generated code author.
         */
        private String author = "generator";

        /**
         * Project root path. Defaults to the current working directory.
         */
        private String projectPath = System.getProperty("user.dir");

        /**
         * Optional module name for multi-module projects.
         */
        private String moduleName = "";

        /**
         * Base Java package.
         */
        private String basePackage = "com.example";

        /**
         * Table prefix removed from generated entity names.
         */
        private String tablePrefix = "";

        /**
         * Included tables. Empty means all tables.
         */
        private List<String> includeTable;

        /**
         * Excluded tables.
         */
        private List<String> excludeTable;

        /**
         * Enables Swagger annotations.
         */
        private boolean enableSwagger = false;

        /**
         * Enables Lombok annotations.
         */
        private boolean enableLombok = true;

        /**
         * Generates REST-style controllers.
         */
        private boolean restController = true;

        /**
         * Primary key generation strategy.
         */
        private IdType idType = IdType.AUTO;

        /**
         * Logical delete column name.
         */
        private String logicDeleteColumn = "deleted";

        /**
         * Auto-fill column rules.
         */
        private List<Column> tableFills = List.of(
                new Column("gmt_create", FieldFill.INSERT),
                new Column("gmt_modified", FieldFill.INSERT_UPDATE),
                new Column("create_by", FieldFill.INSERT),
                new Column("update_by", FieldFill.INSERT_UPDATE));

        /**
         * Configures a MySQL connection.
         */
        public GeneratorConfig mysql(String host, int port, String database) {
            this.url = "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true"
                    .formatted(host, port, database);
            return this;
        }

        /**
         * Configures a PostgreSQL connection.
         */
        public GeneratorConfig postgresql(String host, int port, String database) {
            this.url = "jdbc:postgresql://%s:%d/%s".formatted(host, port, database);
            return this;
        }

        /**
         * Configures database credentials.
         */
        public GeneratorConfig auth(String username, String password) {
            return username(username).password(password);
        }

        /**
         * Configures package and module structure.
         */
        public GeneratorConfig project(String basePackage, String moduleName) {
            return basePackage(basePackage).moduleName(moduleName);
        }

        /**
         * Limits generation to the supplied tables.
         */
        public GeneratorConfig includeTables(String... tables) {
            this.includeTable = List.of(tables);
            return this;
        }

        /**
         * Excludes the supplied tables from generation.
         */
        public GeneratorConfig excludeTables(String... tables) {
            this.excludeTable = List.of(tables);
            return this;
        }

        /**
         * Executes generation with the current settings.
         */
        public void generate() {
            CodeGenerator.execute(this);
        }
    }
}