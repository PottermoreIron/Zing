package com.pot.zing.framework.starter.security.expression;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Set;

/**
 * Permission expression backed by Spring SpEL.
 *
 * <p>The SpEL engine is an implementation detail: the expression string is
 * framework-agnostic from the caller's perspective.</p>
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class SpelPermissionExpression implements PermissionExpression {

    private final String expression;
    private final ExpressionParser parser;

    @Override
    public boolean evaluate(EvaluationContext context) {
        try {
            StandardEvaluationContext spelContext =
                    new StandardEvaluationContext(new PermissionExpressionRoot(context));
            context.getMethodParameters().forEach(spelContext::setVariable);
            spelContext.setVariable("currentUserId", context.getCurrentUserId());
            spelContext.setVariable("userId", context.getCurrentUserId());
            spelContext.setVariable("permissions", context.getUserPermissions());
            spelContext.setVariable("roles", context.getUserRoles());

            Expression spelExpression = parser.parseExpression(expression);
            Boolean result = spelExpression.getValue(spelContext, Boolean.class);
            log.debug("[Permission] SpEL expression evaluated — expression={}, result={}", expression, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("[Permission] SpEL expression evaluation failed — expression={}, error={}", expression,
                    e.getMessage(), e);
            return false;
        }
    }

    @RequiredArgsConstructor
    static final class PermissionExpressionRoot {

        private final EvaluationContext context;

        public boolean hasPermission(String permission) {
            return context.hasPermission(permission);
        }

        public boolean hasPermission(String resource, String action) {
            return context.hasPermission(resource + ":" + action);
        }

        public boolean hasPermission(Object targetId, String resource, String action) {
            if (targetId == null) {
                return false;
            }
            return context.hasPermission(resource + ":" + targetId + ":" + action)
                    || hasPermission(resource, action);
        }

        public boolean hasRole(String role) {
            return context.hasRole(role);
        }

        public String currentUserId() {
            return context.getCurrentUserId();
        }

        public Set<String> permissions() {
            return context.getUserPermissions();
        }

        public Set<String> roles() {
            return context.getUserRoles();
        }
    }
}
