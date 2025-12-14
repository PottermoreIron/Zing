package com.pot.auth.domain.authorization.expression;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * SpEL权限表达式
 *
 * <p>
 * 用于处理SpEL表达式，如：
 * <ul>
 * <li>{@code "hasPermission(#articleId, 'article', 'edit')"}</li>
 * <li>{@code "hasRole('ADMIN') or @customChecker.check(#user)"}</li>
 * <li>{@code "#userId == @currentUserId"}</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
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
            // 创建SpEL评估上下文
            StandardEvaluationContext spelContext = new StandardEvaluationContext();

            // 注入方法参数
            context.getMethodParameters().forEach(spelContext::setVariable);

            // 注入当前用户ID
            spelContext.setVariable("userId", context.getCurrentUserId());
            spelContext.setVariable("currentUserId", context.getCurrentUserId());

            // 注册自定义函数
            spelContext.registerFunction("hasPermission",
                    getClass().getMethod("hasPermission", EvaluationContext.class, String.class));
            spelContext.registerFunction("hasRole",
                    getClass().getMethod("hasRole", EvaluationContext.class, String.class));

            // 解析并评估表达式
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

    /**
     * SpEL中使用的hasPermission函数
     */
    public static boolean hasPermission(PermissionExpression.EvaluationContext context, String permission) {
        return context.hasPermission(permission);
    }

    /**
     * SpEL中使用的hasRole函数
     */
    public static boolean hasRole(PermissionExpression.EvaluationContext context, String role) {
        return context.hasRole(role);
    }

    @Override
    public String toString() {
        return "SpelPermission(" + expression + ")";
    }
}
