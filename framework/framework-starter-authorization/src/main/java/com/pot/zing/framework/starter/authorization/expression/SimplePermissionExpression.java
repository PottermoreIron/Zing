package com.pot.zing.framework.starter.authorization.expression;

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
        boolean hasPermission = context.hasPermission(expression);
        log.debug("[权限评估] 简单权限: expression={}, result={}", expression, hasPermission);
        return hasPermission;
    }
}