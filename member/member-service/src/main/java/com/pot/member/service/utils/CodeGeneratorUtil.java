package com.pot.member.service.utils;

import com.pot.zing.framework.starter.code.generator.service.CodeGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/10/19 20:21
 * @description: 代码生成器工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGeneratorUtil {

    private final CodeGenerationService codeGenerationService;

    /**
     * 根据配置文件生成代码
     */
    public void generate() {
        log.info("开始执行代码生成...");
        try {
            codeGenerationService.generateFromProperties();
            log.info("✅ 代码生成完成");
        } catch (Exception e) {
            log.error("❌ 代码生成失败", e);
            throw e;
        }
    }

    /**
     * 生成指定表的代码
     *
     * @param tables 表名数组
     */
    public void generateTables(String... tables) {
        log.info("开始生成指定表的代码: {}", String.join(", ", tables));
        // 可以通过自定义配置实现
        // 这里简化处理,实际使用可扩展
        generate();
    }
}
