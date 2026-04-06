package com.pot.zing.framework.starter.authorization.security;

import java.util.Set;

/**
 * Reads security-context data required by authorization aspects.
 */
public interface AuthorizationSecurityAccessor {

    String getCurrentUserId();

    Set<String> getCurrentUserPermissions();

    Set<String> getCurrentUserRoles();

    boolean isAuthenticated();
}