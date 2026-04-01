package com.pot.auth.domain.authorization.expression;

import com.pot.auth.domain.shared.enums.Logical;

/**
 * 权限表达式解析器
 *
 * <p>
 * 领域层只定义权限表达式解析能力，不关心具体解析技术。
 */
public interface PermissionExpressionParser {

    PermissionExpression parse(String expressionString);

    PermissionExpression parseMultiple(String[] expressions, Logical logical);
}
