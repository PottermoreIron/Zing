package com.pot.auth.domain.authorization.expression;

import java.util.Map;
import java.util.Set;

/**
 * 权限表达式接口
 *
 * <p>
 * 所有权限表达式的统一抽象
 *
 * @author pot
 * @since 2025-12-14
 */
public interface PermissionExpression {

    /**
     * 评估表达式
     *
     * @param context 评估上下文
     * @return true if满足权限要求
     */
    boolean evaluate(EvaluationContext context);

    /**
     * 获取表达式字符串
     *
     * @return 表达式字符串
     */
    String getExpression();

    /**
     * 评估上下文
     */
    interface EvaluationContext {
        /**
         * 获取用户权限集合
         */
        Set<String> getUserPermissions();

        /**
         * 获取用户角色集合
         */
        Set<String> getUserRoles();

        /**
         * 获取方法参数
         */
        Map<String, Object> getMethodParameters();

        /**
         * 获取当前用户ID
         */
        Long getCurrentUserId();

        /**
         * 检查用户是否拥有指定权限
         */
        boolean hasPermission(String permission);

        /**
         * 检查用户是否拥有指定角色
         */
        boolean hasRole(String role);
    }
}
