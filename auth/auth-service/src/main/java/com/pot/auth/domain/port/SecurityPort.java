package com.pot.auth.domain.port;

import java.util.Map;
import java.util.Set;

public interface SecurityPort {

        String getCurrentUserId();

        Set<String> getCurrentUserPermissions();

        Set<String> getCurrentUserRoles();

        boolean isAuthenticated();

        Map<String, Object> getCurrentUserDetails();

        void clearContext();
}
