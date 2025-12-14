package com.pot.auth.domain.authorization.expression;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Map;
import java.util.Set;

/**
 * 标准权限评估上下文
 *
 * <p>
 * 提供权限评估所需的完整上下文信息：
 * <ul>
 * <li>用户权限集合</li>
 * <li>用户角色集合</li>
 * <li>当前用户ID</li>
 * <li>方法参数</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Getter
@Builder
public class StandardPermissionEvaluationContext implements PermissionExpression.EvaluationContext {

    /**
     * 用户权限集合
     */
    @Singular
    private final Set<String> permissions;

    /**
     * 用户角色集合
     */
    @Singular
    private final Set<String> roles;

    /**
     * 当前用户ID
     */
    private final String currentUserId;

    /**
     * 方法参数 (参数名 -> 参数值)
     */
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
    public Long getCurrentUserId() {
        return currentUserId != null ? Long.parseLong(currentUserId) : null;
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    @Override
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public String getCurrentUserIdString() {
        return currentUserId;
    }

    @Override
    public Map<String, Object> getMethodParameters() {
        return methodParameters;
    }

    /**
     * 创建空上下文
     */
    public static StandardPermissionEvaluationContext empty() {
        return StandardPermissionEvaluationContext.builder().build();
    }

    /**
     * 从权限集合创建上下文
     */
    public static StandardPermissionEvaluationContext fromPermissions(Set<String> permissions) {
        return StandardPermissionEvaluationContext.builder()
                .permissions(permissions)
                .build();
    }

    /**
     * 从权限和角色创建上下文
     */
    public static StandardPermissionEvaluationContext from(Set<String> permissions, Set<String> roles) {
        return StandardPermissionEvaluationContext.builder()
                .permissions(permissions)
                .roles(roles)
                .build();
    }
}
