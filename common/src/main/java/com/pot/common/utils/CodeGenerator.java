package com.pot.common.utils;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;

import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @author: Pot
 * @created: 2025/2/23 21:08
 * @description: 代码生成器
 */
public class CodeGenerator {

    private final static String DATABASE_NAME = "";
    private static final String DATABASE_URL = String.format("jdbc:mysql://localhost:3306/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&remarks=true&useInformationSchema=true&tinyInt1isBit=true", DATABASE_NAME);
    private final static String DATABASE_USER_NAME = "root";
    private final static String DATABASE_PASSWORD = "000802";
    private final static String AUTHOR = "pot";
    private final static String OUTPUT_DIR = Paths.get(System.getProperty("user.dir")) + "/src/main/java";
    private final static String DATE_FORMAT = "yyyy-MM-dd";
    private final static String PACKAGE_PARENT = "";
    private final static String PACKAGE_ENTITY = "entity";
    private final static String PACKAGE_MAPPER = "mapper";
    private final static String PACKAGE_SERVICE = "service";
    private final static String PACKAGE_SERVICE_IMPL = "service.impl";
    private final static String PACKAGE_CONTROLLER = "controller";
    private final static String PACKAGE_XML = "mapper.xml";

    public static void main(String[] args) {
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
                )
                .strategyConfig(builder -> builder
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
