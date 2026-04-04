package com.pot.zing.framework.starter.authorization.expression;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Set;
import java.util.Map;

/**
 * 标准权限评估上下文。
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

    public static StandardPermissionEvaluationContext fromPermissions(Set<String> permissions) {
        return StandardPermissionEvaluationContext.builder()
                .permissions(permissions)
                .build();
    }
}