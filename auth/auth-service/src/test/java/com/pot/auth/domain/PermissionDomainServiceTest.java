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
@DisplayName("PermissionDomainService 单元测试")
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
        @DisplayName("权限非空时，递增版本号、缓存权限、缓存摘要，返回包含版本和摘要的元数据")
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
        @DisplayName("空权限集合时，使用__EMPTY__占位符缓存，返回元数据")
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
        @DisplayName("CachePort.increment 成功，返回新版本号")
        void whenIncrementSuccess_thenReturnNewVersion() {
            when(cachePort.increment(any(), eq(1L), eq(Duration.ZERO))).thenReturn(3L);

            PermissionVersion version = permissionDomainService
                    .incrementPermissionVersion(USER_ID, USER_DOMAIN);

            assertThat(version.value()).isEqualTo(3L);
        }

        @Test
        @DisplayName("CachePort.increment 抛出异常，返回初始版本号（降级）")
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
        @DisplayName("Redis中存在版本号，返回对应版本")
        void whenVersionExists_thenReturnCachedVersion() {
            when(cachePort.get(any(), eq(Long.class))).thenReturn(Optional.of(7L));

            PermissionVersion version = permissionDomainService
                    .getCurrentPermissionVersion(USER_ID, USER_DOMAIN);

            assertThat(version.value()).isEqualTo(7L);
        }

        @Test
        @DisplayName("Redis中不存在版本号，返回初始版本号")
        void whenVersionNotExists_thenReturnInitialVersion() {
            when(cachePort.get(any(), eq(Long.class))).thenReturn(Optional.empty());

            PermissionVersion version = permissionDomainService
                    .getCurrentPermissionVersion(USER_ID, USER_DOMAIN);

            assertThat(version.value()).isEqualTo(PermissionVersion.initial().value());
        }

        @Test
        @DisplayName("Redis抛出异常，降级返回初始版本号")
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
        @DisplayName("缓存命中，返回权限集合")
        void whenCacheHit_thenReturnPermissions() {
            Set<String> permissions = Set.of("member:read", "order:list");
            when(cachePort.get(any(), eq(Set.class))).thenReturn(Optional.of(permissions));

            Set<String> result = permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN);

            assertThat(result).containsExactlyInAnyOrder("member:read", "order:list");
        }

        @Test
        @DisplayName("缓存返回__EMPTY__占位符，过滤后返回空集合")
        void whenCacheHasPlaceholder_thenReturnEmptySet() {
            Set<String> placeholder = Collections.singleton("__EMPTY__");
            when(cachePort.get(any(), eq(Set.class))).thenReturn(Optional.of(placeholder));

            Set<String> result = permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("缓存未命中，返回空集合")
        void whenCacheMiss_thenReturnEmptySet() {
            when(cachePort.get(any(), eq(Set.class))).thenReturn(Optional.empty());

            Set<String> result = permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Redis抛出异常，降级返回空集合")
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
        @DisplayName("同时删除权限Key和摘要Key")
        void whenInvalidate_thenDeleteBothKeys() {
            permissionDomainService.invalidatePermissionCache(USER_ID, USER_DOMAIN);

            verify(cachePort, times(2)).delete(any());
        }

        @Test
        @DisplayName("Redis删除异常时，不抛出异常（静默失败）")
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
        @DisplayName("缓存中的摘要与当前权限匹配，返回true")
        void whenDigestMatches_thenReturnTrue() {
            Set<String> permissions = Set.of("member:read");
            String realDigest = PermissionDigest.from(permissions).value();
            when(cachePort.get(any(), eq(String.class))).thenReturn(Optional.of(realDigest));

            boolean result = permissionDomainService.verifyPermissionDigest(USER_ID, USER_DOMAIN, permissions);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("缓存中的摘要与当前权限不匹配，返回false")
        void whenDigestNotMatch_thenReturnFalse() {
            String fakeDigest = "00000000000000000000000000000000"; // 32位非真实MD5
            when(cachePort.get(any(), eq(String.class))).thenReturn(Optional.of(fakeDigest));

            boolean result = permissionDomainService.verifyPermissionDigest(
                    USER_ID, USER_DOMAIN, Set.of("member:read"));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("缓存中无摘要，降级返回true（放行）")
        void whenNoDigestInCache_thenReturnTrue() {
            when(cachePort.get(any(), eq(String.class))).thenReturn(Optional.empty());

            boolean result = permissionDomainService.verifyPermissionDigest(
                    USER_ID, USER_DOMAIN, Set.of("member:read"));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Redis抛出异常，降级返回true（放行）")
        void whenRedisThrows_thenReturnTrue() {
            when(cachePort.get(any(), any())).thenThrow(new RuntimeException("Redis error"));

            boolean result = permissionDomainService.verifyPermissionDigest(
                    USER_ID, USER_DOMAIN, Set.of("member:read"));

            assertThat(result).isTrue();
        }
    }
}
