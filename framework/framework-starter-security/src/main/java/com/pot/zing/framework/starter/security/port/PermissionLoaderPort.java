package com.pot.zing.framework.starter.security.port;

import java.util.Set;

/**
 * Port for loading user permissions at request time.
 *
 * <p>Downstream services implement this port to provide their own permission
 * loading strategy (e.g., calling auth-service, reading from Redis, etc.).
 * When a bean implementing this interface is registered, the
 * {@link com.pot.zing.framework.starter.security.filter.GatewayHeaderAuthenticationFilter}
 * will call it to populate the security context with the user's permissions
 * on every authenticated request.</p>
 *
 * <p>Implementations are expected to use local caching to avoid remote
 * calls on every request. The {@code permVersion} parameter can be used as
 * part of the cache key to ensure stale permissions are invalidated when
 * auth-service increments the version after a role change.</p>
 */
public interface PermissionLoaderPort {

    /**
     * Loads the current user's permission codes.
     *
     * @param userId      string representation of the user's business ID
     * @param userDomain  domain context (e.g. "member", "admin")
     * @param permVersion version token injected by the gateway; may be {@code null}
     * @return immutable set of permission codes; never {@code null}
     */
    Set<String> loadPermissions(String userId, String userDomain, String permVersion);
}
