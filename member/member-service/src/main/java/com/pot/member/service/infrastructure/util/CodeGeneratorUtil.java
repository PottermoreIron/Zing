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
        log.info("Starting code generation...");
        try {
            codeGenerationService.generateFromProperties();
            log.info("Code generation complete");
        } catch (Exception e) {
            log.error("Code generation failed", e);
            throw e;
        }
    }

        public void generateTables(String... tables) {
        log.info("Starting code generation for tables: {}", String.join(", ", tables));
        generate();
    }
}
