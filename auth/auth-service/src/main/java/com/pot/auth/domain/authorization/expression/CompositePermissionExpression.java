package com.pot.auth.domain.authorization.expression;

import com.pot.auth.domain.shared.enums.Logical;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 组合权限表达式
 *
 * <p>
 * 用于处理多个权限表达式的逻辑组合
 *
 * <p>
 * 示例：
 * <ul>
 * <li>AND: {@code ["user.read", "user.write"]} - 必须同时拥有两个权限</li>
 * <li>OR: {@code ["user.read", "admin.access"]} - 拥有任一权限即可</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class CompositePermissionExpression implements PermissionExpression {

    private final List<PermissionExpression> expressions;
    private final Logical operator;

    @Override
    public boolean evaluate(PermissionExpression.EvaluationContext context) {
        return switch (operator) {
            case AND -> evaluateAnd(context);
            case OR -> evaluateOr(context);
            case NOT -> evaluateNot(context);
        };
    }

    private boolean evaluateAnd(PermissionExpression.EvaluationContext context) {
        boolean result = expressions.stream().allMatch(expr -> expr.evaluate(context));
        log.debug("[权限评估] AND组合: count={}, result={}", expressions.size(), result);
        return result;
    }

    private boolean evaluateOr(PermissionExpression.EvaluationContext context) {
        boolean result = expressions.stream().anyMatch(expr -> expr.evaluate(context));
        log.debug("[权限评估] OR组合: count={}, result={}", expressions.size(), result);
        return result;
    }

    private boolean evaluateNot(PermissionExpression.EvaluationContext context) {
        if (expressions.isEmpty()) {
            return false;
        }
        boolean result = !expressions.getFirst().evaluate(context);
        log.debug("[权限评估] NOT: result={}", result);
        return result;
    }

    @Override
    public String getExpression() {
        return expressions.stream()
                .map(PermissionExpression::getExpression)
                .reduce((a, b) -> a + " " + operator + " " + b)
                .orElse("");
    }

    @Override
    public String toString() {
        return "CompositePermission(" + operator + ", " + expressions + ")";
    }
}
