package com.pot.auth.domain;

import com.pot.auth.domain.authorization.service.PermissionDomainService;
import com.pot.auth.domain.authorization.valueobject.PermissionCacheMetadata;
import com.pot.auth.domain.authorization.valueobject.PermissionDigest;
import com.pot.auth.domain.authorization.valueobject.PermissionVersion;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import com.pot.auth.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionDomainService unit test")
class PermissionDomainServiceTest {

    @Mock
    private CachePort cachePort;

    private PermissionDomainService permissionDomainService;

    private static final UserId USER_ID = TestFixtures.USER_ID;
    private static final UserDomain USER_DOMAIN = TestFixtures.USER_DOMAIN;

    @BeforeEach
    void setUp() {
        permissionDomainService = new PermissionDomainService(cachePort, 3600L);
    }

    // cachePermissionsWithMetadata

    @Nested
    @DisplayName("cachePermissionsWithMetadata()")
    class CachePermissionsWithMetadata {

        @Test
        @DisplayName("Non-empty permissions: increments version, caches permissions and digest, returns metadata")
        void whenPermissionsNotEmpty_thenCacheAndReturnMetadata() {
            Set<String> permissions = Set.of("member:read", "member:write");
            when(cachePort.increment(any(), eq(1L), eq(Duration.ZERO))).thenReturn(5L);

            PermissionCacheMetadata metadata = permissionDomainService
                    .cachePermissionsWithMetadata(USER_ID, USER_DOMAIN, permissions);

            assertThat(metadata.version()).isEqualTo(5L);
            assertThat(metadata.digest()).isNotBlank();
            verify(cachePort).increment(any(), eq(1L), eq(Duration.ZERO));
            verify(cachePort, times(2)).set(any(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("Empty permission set: caches with __EMPTY__ placeholder and returns metadata")
        void whenPermissionsEmpty_thenCacheWithPlaceholderAndReturnMetadata() {
            Set<String> emptyPermissions = Collections.emptySet();
            when(cachePort.increment(any(), eq(1L), eq(Duration.ZERO))).thenReturn(1L);

            PermissionCacheMetadata metadata = permissionDomainService
                    .cachePermissionsWithMetadata(USER_ID, USER_DOMAIN, emptyPermissions);

            assertThat(metadata).isNotNull();
            verify(cachePort, times(2)).set(any(), any(), any(Duration.class));
        }
    }

    // incrementPermissionVersion

    @Nested
    @DisplayName("incrementPermissionVersion(UserId, UserDomain)")
    class IncrementPermissionVersion {

        @Test
        @DisplayName("CachePort.increment success returns new version number")
        void whenIncrementSuccess_thenReturnNewVersion() {
            when(cachePort.increment(any(), eq(1L), eq(Duration.ZERO))).thenReturn(3L);

            PermissionVersion version = permissionDomainService
                    .incrementPermissionVersion(USER_ID, USER_DOMAIN);

            assertThat(version.value()).isEqualTo(3L);
        }

        @Test
        @DisplayName("CachePort.increment exception returns initial version number (degraded)")
        void whenIncrementThrows_thenReturnInitialVersion() {
            when(cachePort.increment(any(), anyLong(), any())).thenThrow(new RuntimeException("Redis down"));

            PermissionVersion version = permissionDomainService
                    .incrementPermissionVersion(USER_ID, USER_DOMAIN);

            assertThat(version.value()).isEqualTo(PermissionVersion.initial().value());
        }
    }

    // getCurrentPermissionVersion

    @Nested
    @DisplayName("getCurrentPermissionVersion()")
    class GetCurrentPermissionVersion {

        @Test
        @DisplayName("Version exists in Redis, returns corresponding version")
        void whenVersionExists_thenReturnCachedVersion() {
            when(cachePort.get(any(), eq(Long.class))).thenReturn(Optional.of(7L));

            PermissionVersion version = permissionDomainService
                    .getCurrentPermissionVersion(USER_ID, USER_DOMAIN);

            assertThat(version.value()).isEqualTo(7L);
        }

        @Test
        @DisplayName("Version not found in Redis, returns initial version number")
        void whenVersionNotExists_thenReturnInitialVersion() {
            when(cachePort.get(any(), eq(Long.class))).thenReturn(Optional.empty());

            PermissionVersion version = permissionDomainService
                    .getCurrentPermissionVersion(USER_ID, USER_DOMAIN);

            assertThat(version.value()).isEqualTo(PermissionVersion.initial().value());
        }

        @Test
        @DisplayName("Redis exception returns initial version number (degraded)")
        void whenRedisThrows_thenReturnInitialVersion() {
            when(cachePort.get(any(), any())).thenThrow(new RuntimeException("connection reset"));

            PermissionVersion version = permissionDomainService
                    .getCurrentPermissionVersion(USER_ID, USER_DOMAIN);

            assertThat(version.value()).isEqualTo(PermissionVersion.initial().value());
        }
    }

    // getCachedPermissions

    @Nested
    @DisplayName("getCachedPermissions()")
    class GetCachedPermissions {

        @Test
        @DisplayName("Cache hit returns permission set")
        void whenCacheHit_thenReturnPermissions() {
            Set<String> permissions = Set.of("member:read", "order:list");
            when(cachePort.get(any(), eq(Set.class))).thenReturn(Optional.of(permissions));

            Set<String> result = permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN);

            assertThat(result).containsExactlyInAnyOrder("member:read", "order:list");
        }

        @Test
        @DisplayName("Cache returns __EMPTY__ placeholder, filtered result is empty set")
        void whenCacheHasPlaceholder_thenReturnEmptySet() {
            Set<String> placeholder = Collections.singleton("__EMPTY__");
            when(cachePort.get(any(), eq(Set.class))).thenReturn(Optional.of(placeholder));

            Set<String> result = permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Cache miss returns empty set")
        void whenCacheMiss_thenReturnEmptySet() {
            when(cachePort.get(any(), eq(Set.class))).thenReturn(Optional.empty());

            Set<String> result = permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Redis exception returns empty set (degraded)")
        void whenRedisThrows_thenReturnEmptySet() {
            when(cachePort.get(any(), any())).thenThrow(new RuntimeException("timeout"));

            Set<String> result = permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN);

            assertThat(result).isEmpty();
        }
    }

    // invalidatePermissionCache

    @Nested
    @DisplayName("invalidatePermissionCache()")
    class InvalidatePermissionCache {

        @Test
        @DisplayName("Deletes both the permission key and the digest key")
        void whenInvalidate_thenDeleteBothKeys() {
            permissionDomainService.invalidatePermissionCache(USER_ID, USER_DOMAIN);

            verify(cachePort, times(2)).delete(any());
        }

        @Test
        @DisplayName("Redis delete exception is swallowed silently")
        void whenDeleteThrows_thenNoException() {
            doThrow(new RuntimeException("Redis error")).when(cachePort).delete(any());

            permissionDomainService.invalidatePermissionCache(USER_ID, USER_DOMAIN);
        }
    }

    // verifyPermissionDigest

    @Nested
    @DisplayName("verifyPermissionDigest()")
    class VerifyPermissionDigest {

        @Test
        @DisplayName("Cached digest matches current permissions, returns true")
        void whenDigestMatches_thenReturnTrue() {
            Set<String> permissions = Set.of("member:read");
            String realDigest = PermissionDigest.from(permissions).value();
            when(cachePort.get(any(), eq(String.class))).thenReturn(Optional.of(realDigest));

            boolean result = permissionDomainService.verifyPermissionDigest(USER_ID, USER_DOMAIN, permissions);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Cached digest does not match current permissions, returns false")
        void whenDigestNotMatch_thenReturnFalse() {
            String fakeDigest = "00000000000000000000000000000000"; // 32-char non-real MD5
            when(cachePort.get(any(), eq(String.class))).thenReturn(Optional.of(fakeDigest));

            boolean result = permissionDomainService.verifyPermissionDigest(
                    USER_ID, USER_DOMAIN, Set.of("member:read"));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("No digest in cache, returns true (degraded, allowing through)")
        void whenNoDigestInCache_thenReturnTrue() {
            when(cachePort.get(any(), eq(String.class))).thenReturn(Optional.empty());

            boolean result = permissionDomainService.verifyPermissionDigest(
                    USER_ID, USER_DOMAIN, Set.of("member:read"));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Redis exception returns true (degraded, allowing through)")
        void whenRedisThrows_thenReturnTrue() {
            when(cachePort.get(any(), any())).thenThrow(new RuntimeException("Redis error"));

            boolean result = permissionDomainService.verifyPermissionDigest(
                    USER_ID, USER_DOMAIN, Set.of("member:read"));

            assertThat(result).isTrue();
        }
    }
}
