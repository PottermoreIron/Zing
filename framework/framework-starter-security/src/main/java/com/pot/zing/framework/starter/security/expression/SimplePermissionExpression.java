package com.pot.zing.framework.starter.security.expression;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Permission expression backed by a single permission string.
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class SimplePermissionExpression implements PermissionExpression {

    private final String expression;

    @Override
    public boolean evaluate(EvaluationContext context) {
        boolean result = context.hasPermission(expression);
        log.debug("[Permission] Simple expression evaluated — expression={}, result={}", expression, result);
        return result;
    }
}
