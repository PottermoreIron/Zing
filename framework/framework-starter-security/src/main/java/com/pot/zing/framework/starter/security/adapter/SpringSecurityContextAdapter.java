package com.pot.zing.framework.starter.security.adapter;

import com.pot.zing.framework.starter.security.port.SecurityContextPort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security implementation of {@link SecurityContextPort}.
 *
 * <p>Reads the current user's identity and permissions from
 * {@link SecurityContextHolder}. When the security framework is replaced,
 * provide a different bean — callers remain unchanged.</p>
 */
public class SpringSecurityContextAdapter implements SecurityContextPort {

    @Override
    public String getCurrentUserId() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object details = auth.getDetails();
        if (details instanceof Map<?, ?> detailsMap) {
            Object userId = detailsMap.get("userId");
            if (userId != null) {
                return userId.toString();
            }
        }
        return auth.getName();
    }

    @Override
    public Set<String> getCurrentUserPermissions() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Collections.emptySet();
        }
        Object details = auth.getDetails();
        if (details instanceof Map<?, ?> detailsMap) {
            Object permissions = detailsMap.get("permissions");
            if (permissions instanceof Set<?> permSet) {
                @SuppressWarnings("unchecked")
                Set<String> typed = (Set<String>) permSet;
                return typed;
            }
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("PERM_"))
                .map(a -> a.substring(5))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getCurrentUserRoles() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Collections.emptySet();
        }
        Object details = auth.getDetails();
        if (details instanceof Map<?, ?> detailsMap) {
            Object roles = detailsMap.get("roles");
            if (roles instanceof Set<?> roleSet) {
                @SuppressWarnings("unchecked")
                Set<String> typed = (Set<String>) roleSet;
                return typed;
            }
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated();
    }

    @Override
    public Map<String, Object> getCurrentUserDetails() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Collections.emptyMap();
        }
        Object details = auth.getDetails();
        if (details instanceof Map<?, ?> detailsMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) detailsMap;
            return new HashMap<>(typed);
        }
        Map<String, Object> basic = new HashMap<>();
        basic.put("userId", auth.getName());
        basic.put("authenticated", auth.isAuthenticated());
        return basic;
    }

    @Override
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
