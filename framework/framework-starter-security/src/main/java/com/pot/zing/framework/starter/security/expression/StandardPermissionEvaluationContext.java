package com.pot.zing.framework.starter.security.expression;

import lombok.Getter;
import lombok.Builder;
import lombok.Singular;

import java.util.Map;
import java.util.Set;

/**
 * Default evaluation context populated during permission checks.
 */
@Getter
@Builder
public class StandardPermissionEvaluationContext implements PermissionExpression.EvaluationContext {

    @Singular
    private final Set<String> permissions;

    @Singular
    private final Set<String> roles;

    private final String currentUserId;

    @Singular
    private final Map<String, Object> methodParameters;

    @Override
    public Set<String> getUserPermissions() {
        return permissions;
    }

    @Override
    public Set<String> getUserRoles() {
        return roles;
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    @Override
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
