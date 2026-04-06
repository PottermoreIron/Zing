package com.pot.zing.framework.starter.code.generator.service;

import com.pot.zing.framework.starter.code.generator.util.CodeGenerator;

/**
 * Service for triggering code generation.
 */
public interface CodeGenerationService {
    /**
     * Generates code from bound starter properties.
     */
    void generateFromProperties();

    /**
     * Generates code from an explicit generator config.
     */
    void generate(CodeGenerator.GeneratorConfig config);
}
