package com.pot.auth.infrastructure.expression;

import com.pot.auth.domain.authorization.expression.PermissionExpression;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * 基于 Spring SpEL 的权限表达式实现。
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class SpelPermissionExpression implements PermissionExpression {

    private final String expression;
    private final ExpressionParser parser;

    @Override
    public boolean evaluate(PermissionExpression.EvaluationContext context) {
        try {
            StandardEvaluationContext spelContext = new StandardEvaluationContext();

            context.getMethodParameters().forEach(spelContext::setVariable);
            spelContext.setVariable("userId", context.getCurrentUserId());
            spelContext.setVariable("currentUserId", context.getCurrentUserId());

            spelContext.registerFunction(
                    "hasPermission",
                    getClass().getMethod("hasPermission", EvaluationContext.class, String.class));
            spelContext.registerFunction(
                    "hasRole",
                    getClass().getMethod("hasRole", EvaluationContext.class, String.class));

            Expression exp = parser.parseExpression(expression);
            Boolean result = exp.getValue(spelContext, Boolean.class);

            log.debug("[权限评估] SpEL表达式: expression={}, result={}", expression, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("[权限评估] SpEL表达式评估失败: expression={}, error={}",
                    expression, e.getMessage(), e);
            return false;
        }
    }

    public static boolean hasPermission(PermissionExpression.EvaluationContext context, String permission) {
        return context.hasPermission(permission);
    }

    public static boolean hasRole(PermissionExpression.EvaluationContext context, String role) {
        return context.hasRole(role);
    }

    @Override
    public String toString() {
        return "SpelPermission(" + expression + ")";
    }
}