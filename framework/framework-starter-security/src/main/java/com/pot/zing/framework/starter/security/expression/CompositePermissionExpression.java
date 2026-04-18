package com.pot.zing.framework.starter.security.expression;

import com.pot.zing.framework.starter.security.enums.Logical;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Composite permission expression that applies a logical operator across multiple sub-expressions.
 */
@Getter
@RequiredArgsConstructor
public class CompositePermissionExpression implements PermissionExpression {

    private final List<PermissionExpression> expressions;
    private final Logical operator;

    @Override
    public boolean evaluate(EvaluationContext context) {
        return switch (operator) {
            case AND -> expressions.stream().allMatch(e -> e.evaluate(context));
            case OR  -> expressions.stream().anyMatch(e -> e.evaluate(context));
            case NOT -> !expressions.isEmpty() && !expressions.getFirst().evaluate(context);
        };
    }

    @Override
    public String getExpression() {
        return expressions.stream()
                .map(PermissionExpression::getExpression)
                .reduce((l, r) -> l + " " + operator + " " + r)
                .orElse("");
    }
}
