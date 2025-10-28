package com.pot.zing.framework.starter.code.generator.service;

import com.pot.zing.framework.starter.code.generator.util.CodeGenerator;

/**
 * @author: Pot
 * @created: 2025/10/18 21:31
 * @description: 代码生成器服务
 */
public interface CodeGenerationService {
    /**
     * 使用 starter 中的 `code.generator.*` 配置触发生成
     */
    void generateFromProperties();

    /**
     * 使用自定义的 CodeGenerator.GeneratorConfig 触发生成
     */
    void generate(CodeGenerator.GeneratorConfig config);
}
