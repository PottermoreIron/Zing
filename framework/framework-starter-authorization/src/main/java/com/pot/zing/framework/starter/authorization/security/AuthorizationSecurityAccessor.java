package com.pot.zing.framework.starter.authorization.security;

import java.util.Set;

/**
 * 授权切面需要的安全上下文读取接口。
 */
public interface AuthorizationSecurityAccessor {

    String getCurrentUserId();

    Set<String> getCurrentUserPermissions();

    Set<String> getCurrentUserRoles();

    boolean isAuthenticated();
}