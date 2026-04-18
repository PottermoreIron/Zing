package com.pot.zing.framework.starter.security.port;

import java.util.Map;
import java.util.Set;

/**
 * Anti-corruption port for reading the current security context.
 *
 * <p>All authorization logic depends on this interface, not on any concrete
 * security framework. Swap implementations (Spring Security, Shiro, etc.)
 * by providing a different bean without changing callers.</p>
 */
public interface SecurityContextPort {

    /** Returns the authenticated user's ID, or {@code null} when unauthenticated. */
    String getCurrentUserId();

    /** Returns the current user's permission codes. Empty when unauthenticated. */
    Set<String> getCurrentUserPermissions();

    /** Returns the current user's role codes. Empty when unauthenticated. */
    Set<String> getCurrentUserRoles();

    /** Returns {@code true} when the current request carries a verified identity. */
    boolean isAuthenticated();

    /** Returns a map of supplementary user details (userId, userDomain, etc.). */
    Map<String, Object> getCurrentUserDetails();

    /** Clears the current security context. */
    void clearContext();
}
