package com.pot.zing.framework.starter.security.expression;

import java.util.Map;
import java.util.Set;

/**
 * Contract for permission expressions evaluated at runtime.
 */
public interface PermissionExpression {

    boolean evaluate(EvaluationContext context);

    String getExpression();

    interface EvaluationContext {

        Set<String> getUserPermissions();

        Set<String> getUserRoles();

        Map<String, Object> getMethodParameters();

        String getCurrentUserId();

        boolean hasPermission(String permission);

        boolean hasRole(String role);
    }
}
