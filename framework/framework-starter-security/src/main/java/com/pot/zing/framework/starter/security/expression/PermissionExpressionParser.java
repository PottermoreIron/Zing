package com.pot.zing.framework.starter.security.expression;

import com.pot.zing.framework.starter.security.enums.Logical;

/**
 * Parses raw permission expression strings into evaluable {@link PermissionExpression} objects.
 */
public interface PermissionExpressionParser {

    PermissionExpression parse(String expressionString);

    PermissionExpression parseMultiple(String[] expressions, Logical logical);
}
