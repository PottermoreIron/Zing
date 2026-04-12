package com.pot.auth.infrastructure.adapter.security;

import com.pot.auth.domain.authorization.service.PermissionDomainService;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalCachePermissionQueryAdapter")
class LocalCachePermissionQueryAdapterTest {

    private static final UserId USER_ID = UserId.of(10001L);
    private static final UserDomain USER_DOMAIN = UserDomain.MEMBER;

    @Mock
    private PermissionDomainService permissionDomainService;

    @Nested
    @DisplayName("getCachedPermissions()")
    class GetCachedPermissions {

        @Test
        @DisplayName("L1 cache hit skips domain service")
        void getCachedPermissions_l1Hit_doNotCallDomainServiceAgain() {
            LocalCachePermissionQueryAdapter adapter = new LocalCachePermissionQueryAdapter(permissionDomainService);
            Set<String> permissions = Set.of("member:read", "member:write");
            when(permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN)).thenReturn(permissions);

            Set<String> first = adapter.getCachedPermissions(USER_ID, USER_DOMAIN);
            Set<String> second = adapter.getCachedPermissions(USER_ID, USER_DOMAIN);

            assertThat(first).containsExactlyInAnyOrder("member:read", "member:write");
            assertThat(second).containsExactlyInAnyOrder("member:read", "member:write");
            verify(permissionDomainService, times(1)).getCachedPermissions(USER_ID, USER_DOMAIN);
        }

        @Test
        @DisplayName("Empty collection from domain service is not written to L1")
        void getCachedPermissions_emptyResult_doNotCacheInL1() {
            LocalCachePermissionQueryAdapter adapter = new LocalCachePermissionQueryAdapter(permissionDomainService);
            when(permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN)).thenReturn(Set.of());

            Set<String> first = adapter.getCachedPermissions(USER_ID, USER_DOMAIN);
            Set<String> second = adapter.getCachedPermissions(USER_ID, USER_DOMAIN);

            assertThat(first).isEmpty();
            assertThat(second).isEmpty();
            verify(permissionDomainService, times(2)).getCachedPermissions(USER_ID, USER_DOMAIN);
        }
    }

    @Nested
    @DisplayName("invalidateLocalCache()")
    class InvalidateLocalCache {

        @Test
        @DisplayName("After clearing user's local cache, domain service is queried again")
        void invalidateLocalCache_afterInvalidate_callDomainServiceAgain() {
            LocalCachePermissionQueryAdapter adapter = new LocalCachePermissionQueryAdapter(permissionDomainService);
            Set<String> firstPermissions = Set.of("member:read");
            Set<String> secondPermissions = Set.of("member:read", "member:write");
            when(permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN))
                    .thenReturn(firstPermissions)
                    .thenReturn(secondPermissions);

            Set<String> first = adapter.getCachedPermissions(USER_ID, USER_DOMAIN);
            adapter.invalidateLocalCache(USER_ID, USER_DOMAIN);
            Set<String> second = adapter.getCachedPermissions(USER_ID, USER_DOMAIN);

            assertThat(first).containsExactly("member:read");
            assertThat(second).containsExactlyInAnyOrder("member:read", "member:write");
            verify(permissionDomainService, times(2)).getCachedPermissions(USER_ID, USER_DOMAIN);
        }
    }
}