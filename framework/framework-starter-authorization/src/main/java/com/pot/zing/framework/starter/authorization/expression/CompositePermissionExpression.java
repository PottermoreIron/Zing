package com.pot.zing.framework.starter.authorization.expression;

import com.pot.zing.framework.starter.authorization.enums.Logical;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 组合权限表达式。
 */
@Getter
@RequiredArgsConstructor
public class CompositePermissionExpression implements PermissionExpression {

    private final List<PermissionExpression> expressions;
    private final Logical operator;

    @Override
    public boolean evaluate(EvaluationContext context) {
        return switch (operator) {
            case AND -> expressions.stream().allMatch(expression -> expression.evaluate(context));
            case OR -> expressions.stream().anyMatch(expression -> expression.evaluate(context));
            case NOT -> !expressions.isEmpty() && !expressions.getFirst().evaluate(context);
        };
    }

    @Override
    public String getExpression() {
        return expressions.stream()
                .map(PermissionExpression::getExpression)
                .reduce((left, right) -> left + " " + operator + " " + right)
                .orElse("");
    }
}