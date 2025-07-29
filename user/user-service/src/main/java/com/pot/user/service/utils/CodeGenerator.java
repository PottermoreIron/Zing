package com.pot.user.service.utils;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;
import org.apache.ibatis.type.JdbcType;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author: Pot
 * @created: 2025/2/23 22:53
 * @description: 代码生成器
 */
public class CodeGenerator {

    private final static String DATABASE_NAME = "user";
    private static final String DATABASE_URL = String.format("jdbc:mysql://localhost:3306/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&remarks=true&useInformationSchema=true&tinyInt1isBit=true", DATABASE_NAME);
    private final static String DATABASE_USER_NAME = "root";
    private final static String DATABASE_PASSWORD = "000802";
    private final static String AUTHOR = "pot";
    private final static String OUTPUT_DIR = Paths.get(System.getProperty("user.dir")) + "/user/service/src/main/java";
    private final static String DATE_FORMAT = "yyyy-MM-dd";
    private final static String PACKAGE_PARENT = "com.pot.user.service";
    private final static String PACKAGE_ENTITY = "entity";
    private final static String PACKAGE_MAPPER = "mapper";
    private final static String PACKAGE_SERVICE = "service";
    private final static String PACKAGE_SERVICE_IMPL = "service.impl";
    private final static String PACKAGE_CONTROLLER = "controller";
    private final static String PACKAGE_XML = "mapper.xml";
    private final static boolean ENABLE_PACKAGE_OVERRIDE = false;

    public static void main(String[] args) {
        System.out.println(OUTPUT_DIR);
        FastAutoGenerator.create(DATABASE_URL, DATABASE_USER_NAME, DATABASE_PASSWORD)
                .globalConfig(builder -> builder
                        .author(AUTHOR)
                        .outputDir(OUTPUT_DIR)
                        .enableSwagger()
                        .commentDate(DATE_FORMAT)
                )
                .packageConfig(builder -> builder
                        .parent(PACKAGE_PARENT)
                        .entity(PACKAGE_ENTITY)
                        .mapper(PACKAGE_MAPPER)
                        .service(PACKAGE_SERVICE)
                        .serviceImpl(PACKAGE_SERVICE_IMPL)
                        .controller(PACKAGE_CONTROLLER)
                        .xml(PACKAGE_XML)
                        .pathInfo(Collections.singletonMap(OutputFile.xml, Paths.get(System.getProperty("user.dir")) + "/user/service/src/main/resources/mapper")
                        )
                )
                .dataSourceConfig(builder ->
                        builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                            // 兼容旧版本转换成Integer
                            if (JdbcType.TINYINT == metaInfo.getJdbcType()) {
                                return DbColumnType.INTEGER;
                            }
                            return typeRegistry.getColumnType(metaInfo);
                        })
                )
                .strategyConfig(builder -> builder
                        .addTablePrefix("t_")
                        .entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .enableRemoveIsPrefix()
                        .logicDeleteColumnName("deleted")
                        .addTableFills(Arrays.asList(
                                new Column("gmt_create", FieldFill.INSERT),
                                new Column("gmt_modified", FieldFill.INSERT_UPDATE)
                        ))
                        .idType(IdType.AUTO)
                        .controllerBuilder()
                        .enableRestStyle()
                )
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}
