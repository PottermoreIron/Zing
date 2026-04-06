package com.pot.zing.framework.starter.authorization.expression;

import com.pot.zing.framework.starter.authorization.enums.Logical;

/**
 * Parses permission expressions.
 */
public interface PermissionExpressionParser {

    PermissionExpression parse(String expressionString);

    PermissionExpression parseMultiple(String[] expressions, Logical logical);
}