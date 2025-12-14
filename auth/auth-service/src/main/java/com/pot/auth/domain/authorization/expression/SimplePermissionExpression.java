package com.pot.auth.domain.authorization.expression;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 简单权限表达式
 *
 * <p>
 * 用于处理简单的权限字符串，如：
 * <ul>
 * <li>{@code "user.read"}</li>
 * <li>{@code "article:123:edit"}</li>
 * <li>{@code "order:own:*"}</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class SimplePermissionExpression implements PermissionExpression {

    private final String expression;

    @Override
    public boolean evaluate(PermissionExpression.EvaluationContext context) {
        boolean hasPermission = context.hasPermission(expression);
        log.debug("[权限评估] 简单权限: expression={}, result={}", expression, hasPermission);
        return hasPermission;
    }

    @Override
    public String toString() {
        return "SimplePermission(" + expression + ")";
    }
}
