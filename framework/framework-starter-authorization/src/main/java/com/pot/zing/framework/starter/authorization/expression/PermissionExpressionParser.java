package com.pot.zing.framework.starter.authorization.expression;

import com.pot.zing.framework.starter.authorization.enums.Logical;

/**
 * 权限表达式解析器。
 */
public interface PermissionExpressionParser {

    PermissionExpression parse(String expressionString);

    PermissionExpression parseMultiple(String[] expressions, Logical logical);
}