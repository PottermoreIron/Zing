package com.pot.zing.framework.starter.code.generator.service.impl;

import com.pot.zing.framework.starter.code.generator.properties.CodeGeneratorProperties;
import com.pot.zing.framework.starter.code.generator.service.CodeGenerationService;
import com.pot.zing.framework.starter.code.generator.util.CodeGenerator;
import lombok.RequiredArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/18 21:31
 * @description: 默认代码生成器接口实现类
 */
@RequiredArgsConstructor
public class DefaultCodeGenerationService implements CodeGenerationService {
    private final CodeGeneratorProperties properties;

    @Override
    public void generateFromProperties() {
        CodeGenerator.GeneratorConfig cfg = properties.toGeneratorConfig();
        cfg.generate();
    }

    @Override
    public void generate(CodeGenerator.GeneratorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        config.generate();
    }
}
