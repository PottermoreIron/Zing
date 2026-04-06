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
 * MyBatis-Plus 代码生成器工具类
 *
 * <p>提供简洁的链式配置API，支持MySQL和PostgreSQL数据库</p>
 * <p>功能特性：</p>
 * <ul>
 *   <li>支持多数据库类型（MySQL、PostgreSQL）</li>
 *   <li>灵活的表过滤配置</li>
 *   <li>自动字段填充配置</li>
 *   <li>Swagger和Lombok集成</li>
 *   <li>模块化项目结构支持</li>
 * </ul>
 *
 * @author Pot
 * @since 2025-02-23
 */
public final class CodeGenerator {

    private CodeGenerator() {
    }

    /**
     * 创建代码生成器配置实例
     *
     * @return 新的配置实例
     */
    public static GeneratorConfig create() {
        return new GeneratorConfig();
    }

    /**
     * 快速生成代码 - 使用本地MySQL默认配置
     *
     * @param database    数据库名称
     * @param basePackage 基础包名
     */
    public static void quickGenerate(String database, String basePackage) {
        quickGenerate("localhost", database, "root", "", basePackage);
    }

    /**
     * 快速生成代码 - 指定MySQL连接信息
     *
     * @param host        MySQL主机地址
     * @param database    数据库名称
     * @param username    用户名
     * @param password    密码
     * @param basePackage 基础包名
     */
    public static void quickGenerate(String host, String database, String username, String password, String basePackage) {
        create()
                .mysql(host, 3306, database)
                .auth(username, password)
                .basePackage(basePackage)
                .generate();
    }

    /**
     * 执行代码生成核心逻辑
     *
     * @param config 生成配置
     */
    static void execute(GeneratorConfig config) {
        // 验证配置参数
        validateConfig(config);

        // 构建输出目录
        String outputDir = buildOutputDir(config);
        String xmlOutputDir = buildXmlOutputDir(config);

        FastAutoGenerator.create(config.url(), config.username(), config.password())
                // 全局配置
                .globalConfig(builder -> {
                    builder.author(config.author())
                            .outputDir(outputDir)
                            .commentDate("yyyy-MM-dd HH:mm:ss")
                            .disableOpenDir();

                    if (config.enableSwagger()) {
                        builder.enableSwagger();
                    }
                })
                // 包配置
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
                // 数据源配置
                .dataSourceConfig(builder -> {
                    builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                        // 自定义数据库类型转换
                        return switch (metaInfo.getJdbcType()) {
                            case TINYINT -> DbColumnType.INTEGER;  // TINYINT转为Integer
                            case BIT -> DbColumnType.BOOLEAN;      // BIT转为Boolean
                            default -> typeRegistry.getColumnType(metaInfo);
                        };
                    });
                })
                // 策略配置
                .strategyConfig(builder -> {
                    configureTableStrategy(builder, config);
                    configureEntityStrategy(builder, config);
                    configureControllerStrategy(builder, config);
                    configureServiceStrategy(builder);
                    configureMapperStrategy(builder);
                })
                .templateEngine(new VelocityTemplateEngine())
                .execute();

        // 输出生成结果
        System.out.println("✅ 代码生成完成");
        System.out.println("📁 Java输出目录: " + outputDir);
        System.out.println("📁 XML输出目录: " + xmlOutputDir);
    }

    /**
     * 配置表策略
     *
     * @param builder 策略配置构建器
     * @param config  生成配置
     */
    private static void configureTableStrategy(com.baomidou.mybatisplus.generator.config.StrategyConfig.Builder builder, GeneratorConfig config) {
        // 配置表前缀
        if (StringUtils.hasText(config.tablePrefix())) {
            builder.addTablePrefix(config.tablePrefix());
        }

        // 配置包含的表
        if (!CollectionUtils.isEmpty(config.includeTable())) {
            builder.addInclude(config.includeTable());
        }

        // 配置排除的表
        if (!CollectionUtils.isEmpty(config.excludeTable())) {
            builder.addExclude(config.excludeTable());
        }
    }

    /**
     * 配置实体类策略
     *
     * @param builder 策略配置构建器
     * @param config  生成配置
     */
    private static void configureEntityStrategy(com.baomidou.mybatisplus.generator.config.StrategyConfig.Builder builder, GeneratorConfig config) {
        var entityBuilder = builder.entityBuilder()
                // 生成字段注解
                .enableTableFieldAnnotation()
                // 移除布尔字段的is前缀
                .enableRemoveIsPrefix()
                // 设置主键生成策略
                .idType(config.idType())
                // 实体类文件名格式
                .formatFileName("%s");

        // 启用Lombok
        if (config.enableLombok()) {
            entityBuilder.enableLombok();
        }

        // 配置逻辑删除字段
        if (StringUtils.hasText(config.logicDeleteColumn())) {
            entityBuilder.logicDeleteColumnName(config.logicDeleteColumn());
        }

        // 配置字段自动填充
        if (!CollectionUtils.isEmpty(config.tableFills())) {
            config.tableFills().forEach(entityBuilder::addTableFills);
        }
    }

    /**
     * 配置控制器策略
     *
     * @param builder 策略配置构建器
     * @param config  生成配置
     */
    private static void configureControllerStrategy(com.baomidou.mybatisplus.generator.config.StrategyConfig.Builder builder, GeneratorConfig config) {
        var controllerBuilder = builder.controllerBuilder()
                .formatFileName("%sController");

        // 启用REST风格
        if (config.restController()) {
            controllerBuilder.enableRestStyle();
        }
    }

    /**
     * 配置服务层策略
     *
     * @param builder 策略配置构建器
     */
    private static void configureServiceStrategy(com.baomidou.mybatisplus.generator.config.StrategyConfig.Builder builder) {
        builder.serviceBuilder()
                .formatServiceFileName("%sService")
                .formatServiceImplFileName("%sServiceImpl");
    }

    /**
     * 配置Mapper策略
     *
     * @param builder 策略配置构建器
     */
    private static void configureMapperStrategy(com.baomidou.mybatisplus.generator.config.StrategyConfig.Builder builder) {
        builder.mapperBuilder()
                .enableBaseResultMap()      // 生成基础结果映射
                .enableBaseColumnList()     // 生成基础列列表
                .formatMapperFileName("%sMapper")
                .formatXmlFileName("%sMapper");
    }

    /**
     * 构建Java代码输出目录
     *
     * @param config 生成配置
     * @return Java代码输出路径
     */
    private static String buildOutputDir(GeneratorConfig config) {
        String basePath = config.projectPath();
        if (StringUtils.hasText(config.moduleName())) {
            basePath = Paths.get(basePath, config.moduleName()).toString();
        }
        return Paths.get(basePath, "src", "main", "java").toString();
    }

    /**
     * 构建XML文件输出目录
     *
     * @param config 生成配置
     * @return XML文件输出路径
     */
    private static String buildXmlOutputDir(GeneratorConfig config) {
        String basePath = config.projectPath();
        if (StringUtils.hasText(config.moduleName())) {
            basePath = Paths.get(basePath, config.moduleName()).toString();
        }
        return Paths.get(basePath, "src", "main", "resources", "mapper").toString();
    }

    /**
     * 验证生成配置参数
     *
     * @param config 生成配置
     * @throws IllegalArgumentException 当配置参数无效时
     */
    private static void validateConfig(GeneratorConfig config) {
        if (!StringUtils.hasText(config.url())) {
            throw new IllegalArgumentException("❌ 数据库连接URL不能为空");
        }
        if (!StringUtils.hasText(config.username())) {
            throw new IllegalArgumentException("❌ 数据库用户名不能为空");
        }
        if (config.password() == null) {
            throw new IllegalArgumentException("❌ 数据库密码不能为null");
        }
        if (!StringUtils.hasText(config.basePackage())) {
            throw new IllegalArgumentException("❌ 基础包名不能为空");
        }
    }

    /**
     * 代码生成配置类
     *
     * <p>使用链式调用模式，提供流畅的配置体验</p>
     *
     * <h3>使用示例:</h3>
     * <pre>{@code
     * CodeGenerator.create()
     *     .mysql("localhost", 3306, "test_db")
     *     .auth("root", "password")
     *     .project("com.example", "user-service")
     *     .includeTables("user", "role")
     *     .generate();
     * }</pre>
     */
    @Data
    @Accessors(chain = true, fluent = true)
    public static class GeneratorConfig {

        /**
         * 数据库连接URL
         */
        private String url;
        /**
         * 数据库用户名
         */
        private String username;
        /**
         * 数据库密码
         */
        private String password;

        /**
         * 代码作者名称
         */
        private String author = "generator";
        /**
         * 项目根路径，默认为当前工作目录
         */
        private String projectPath = System.getProperty("user.dir");
        /**
         * 模块名称，用于多模块项目
         */
        private String moduleName = "";
        /**
         * 基础包名
         */
        private String basePackage = "com.example";

        /**
         * 表名前缀，生成实体类时会移除此前缀
         */
        private String tablePrefix = "";
        /**
         * 需要生成的表名列表，为空则生成所有表
         */
        private List<String> includeTable;
        /**
         * 需要排除的表名列表
         */
        private List<String> excludeTable;

        /**
         * 是否启用Swagger注解
         */
        private boolean enableSwagger = false;
        /**
         * 是否启用Lombok注解
         */
        private boolean enableLombok = true;
        /**
         * 是否生成REST风格的Controller
         */
        private boolean restController = true;
        /**
         * 主键生成策略
         */
        private IdType idType = IdType.AUTO;
        /**
         * 逻辑删除字段名
         */
        private String logicDeleteColumn = "deleted";

        /**
         * 字段自动填充规则列表
         */
        private List<Column> tableFills = List.of(
                new Column("gmt_create", FieldFill.INSERT),
                new Column("gmt_modified", FieldFill.INSERT_UPDATE),
                new Column("create_by", FieldFill.INSERT),
                new Column("update_by", FieldFill.INSERT_UPDATE)
        );

        /**
         * 配置MySQL数据库连接
         *
         * @param host     数据库主机地址
         * @param port     数据库端口号
         * @param database 数据库名称
         * @return 当前配置实例，支持链式调用
         */
        public GeneratorConfig mysql(String host, int port, String database) {
            this.url = "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true"
                    .formatted(host, port, database);
            return this;
        }

        /**
         * 配置PostgreSQL数据库连接
         *
         * @param host     数据库主机地址
         * @param port     数据库端口号
         * @param database 数据库名称
         * @return 当前配置实例，支持链式调用
         */
        public GeneratorConfig postgresql(String host, int port, String database) {
            this.url = "jdbc:postgresql://%s:%d/%s".formatted(host, port, database);
            return this;
        }

        /**
         * 配置数据库认证信息
         *
         * @param username 数据库用户名
         * @param password 数据库密码
         * @return 当前配置实例，支持链式调用
         */
        public GeneratorConfig auth(String username, String password) {
            return username(username).password(password);
        }

        /**
         * 配置项目结构信息
         *
         * @param basePackage 基础包名，如：com.example
         * @param moduleName  模块名称，用于多模块项目
         * @return 当前配置实例，支持链式调用
         */
        public GeneratorConfig project(String basePackage, String moduleName) {
            return basePackage(basePackage).moduleName(moduleName);
        }

        /**
         * 指定需要生成代码的表
         *
         * @param tables 表名数组
         * @return 当前配置实例，支持链式调用
         */
        public GeneratorConfig includeTables(String... tables) {
            this.includeTable = List.of(tables);
            return this;
        }

        /**
         * 指定需要排除的表
         *
         * @param tables 表名数组
         * @return 当前配置实例，支持链式调用
         */
        public GeneratorConfig excludeTables(String... tables) {
            this.excludeTable = List.of(tables);
            return this;
        }

        /**
         * 执行代码生成
         *
         * <p>根据当前配置生成实体类、Mapper、Service、Controller等代码文件</p>
         */
        public void generate() {
            CodeGenerator.execute(this);
        }
    }
}