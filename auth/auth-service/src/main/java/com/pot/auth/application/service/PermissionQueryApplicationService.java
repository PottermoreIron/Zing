package com.pot.auth.application.service;

import com.pot.auth.domain.port.PermissionQueryPort;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Application service for querying cached user permissions.
 *
 * <p>
 * Delegates to {@link PermissionQueryPort} which provides a two-level cache
 * (Caffeine L1 + Redis L2) populated at login time.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PermissionQueryApplicationService {

    private final PermissionQueryPort permissionQueryPort;

    public Set<String> getCachedPermissions(Long userId, String domain) {
        return permissionQueryPort.getCachedPermissions(
                UserId.of(userId),
                UserDomain.fromCode(domain));
    }
}
