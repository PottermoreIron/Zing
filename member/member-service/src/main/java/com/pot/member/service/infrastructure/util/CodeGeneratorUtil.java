package com.pot.member.service.infrastructure.util;

import com.pot.zing.framework.starter.code.generator.service.CodeGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGeneratorUtil {

    private final CodeGenerationService codeGenerationService;

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

        public void generateTables(String... tables) {
        log.info("开始生成指定表的代码: {}", String.join(", ", tables));
        generate();
    }
}
