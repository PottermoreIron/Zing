package com.pot.auth.infrastructure.adapter.security;

import com.pot.auth.domain.port.SecurityPort;
import com.pot.zing.framework.starter.authorization.security.AuthorizationSecurityAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapts Spring Security authentication state to the domain security port.
 */
@Component
public class SpringSecurityPortAdapter implements SecurityPort, AuthorizationSecurityAccessor {

    @Override
    public String getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // Prefer details when the login flow stored richer user context.
        Object details = authentication.getDetails();
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> detailsMap = (Map<String, Object>) details;
            Object userId = detailsMap.get("userId");
            if (userId != null) {
                return userId.toString();
            }
        }

        // Fall back to the authentication name when no explicit userId is available.
        return authentication.getName();
    }

    @Override
    public Set<String> getCurrentUserPermissions() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptySet();
        }

        // Prefer permissions captured during authentication when available.
        Object details = authentication.getDetails();
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> detailsMap = (Map<String, Object>) details;
            Object permissions = detailsMap.get("permissions");
            if (permissions instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> permissionSet = (Set<String>) permissions;
                return permissionSet;
            }
        }

        // Fall back to authorities with the PERM_ prefix.
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("PERM_"))
                .map(authority -> authority.substring(5))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getCurrentUserRoles() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptySet();
        }

        // Prefer roles captured during authentication when available.
        Object details = authentication.getDetails();
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> detailsMap = (Map<String, Object>) details;
            Object roles = detailsMap.get("roles");
            if (roles instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> roleSet = (Set<String>) roles;
                return roleSet;
            }
        }

        // Fall back to authorities with the ROLE_ prefix.
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    @Override
    public Map<String, Object> getCurrentUserDetails() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyMap();
        }

        Object details = authentication.getDetails();
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> detailsMap = (Map<String, Object>) details;
            return new HashMap<>(detailsMap);
        }

        // Preserve both keys for legacy callers that still expect username.
        Map<String, Object> basicDetails = new HashMap<>();
        basicDetails.put("nickname", authentication.getName());
        basicDetails.put("username", authentication.getName());
        basicDetails.put("authenticated", authentication.isAuthenticated());
        return basicDetails;
    }

    @Override
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Returns the current Spring Security authentication.
     */
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
