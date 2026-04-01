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

/**
 * PermissionDomainService 单元测试
 *
 * <p>
 * 验证：
 * <ul>
 * <li>cachePermissionsWithMetadata：计算摘要、递增版本号、缓存权限和摘要，返回元数据</li>
 * <li>incrementPermissionVersion：正确递增并返回新版本号</li>
 * <li>getCurrentPermissionVersion：命中缓存时返回缓存版本，否则返回初始版本</li>
 * <li>getCachedPermissions：命中缓存时返回权限，空占位符被过滤，未命中返回空集合</li>
 * <li>invalidatePermissionCache：删除权限Key和摘要Key</li>
 * <li>verifyPermissionDigest：匹配时返回true，不匹配时返回false，无缓存时降级返回true</li>
 * </ul>
 *
 * @author pot
 */
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

    // ================================================================
    // cachePermissionsWithMetadata
    // ================================================================

    @Nested
    @DisplayName("cachePermissionsWithMetadata()")
    class CachePermissionsWithMetadata {

        @Test
        @DisplayName("权限非空时，递增版本号、缓存权限、缓存摘要，返回包含版本和摘要的元数据")
        void whenPermissionsNotEmpty_thenCacheAndReturnMetadata() {
            // given
            Set<String> permissions = Set.of("member:read", "member:write");
            when(cachePort.increment(any(), eq(1L), eq(Duration.ZERO))).thenReturn(5L);

            // when
            PermissionCacheMetadata metadata = permissionDomainService
                    .cachePermissionsWithMetadata(USER_ID, USER_DOMAIN, permissions);

            // then: 版本号正确
            assertThat(metadata.version()).isEqualTo(5L);
            // then: 摘要非空
            assertThat(metadata.digest()).isNotBlank();

            // then: 调用了increment（版本递增）、set×2（权限+摘要缓存）
            verify(cachePort).increment(any(), eq(1L), eq(Duration.ZERO));
            verify(cachePort, times(2)).set(any(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("空权限集合时，使用__EMPTY__占位符缓存，返回元数据")
        void whenPermissionsEmpty_thenCacheWithPlaceholderAndReturnMetadata() {
            // given
            Set<String> emptyPermissions = Collections.emptySet();
            when(cachePort.increment(any(), eq(1L), eq(Duration.ZERO))).thenReturn(1L);

            // when
            PermissionCacheMetadata metadata = permissionDomainService
                    .cachePermissionsWithMetadata(USER_ID, USER_DOMAIN, emptyPermissions);

            // then
            assertThat(metadata).isNotNull();
            verify(cachePort, times(2)).set(any(), any(), any(Duration.class));
        }
    }

    // ================================================================
    // incrementPermissionVersion
    // ================================================================

    @Nested
    @DisplayName("incrementPermissionVersion(UserId, UserDomain)")
    class IncrementPermissionVersion {

        @Test
        @DisplayName("CachePort.increment 成功，返回新版本号")
        void whenIncrementSuccess_thenReturnNewVersion() {
            // given
            when(cachePort.increment(any(), eq(1L), eq(Duration.ZERO))).thenReturn(3L);

            // when
            PermissionVersion version = permissionDomainService
                    .incrementPermissionVersion(USER_ID, USER_DOMAIN);

            // then
            assertThat(version.value()).isEqualTo(3L);
        }

        @Test
        @DisplayName("CachePort.increment 抛出异常，返回初始版本号（降级）")
        void whenIncrementThrows_thenReturnInitialVersion() {
            // given
            when(cachePort.increment(any(), anyLong(), any())).thenThrow(new RuntimeException("Redis down"));

            // when
            PermissionVersion version = permissionDomainService
                    .incrementPermissionVersion(USER_ID, USER_DOMAIN);

            // then: 降级为初始版本号
            assertThat(version.value()).isEqualTo(PermissionVersion.initial().value());
        }
    }

    // ================================================================
    // getCurrentPermissionVersion
    // ================================================================

    @Nested
    @DisplayName("getCurrentPermissionVersion()")
    class GetCurrentPermissionVersion {

        @Test
        @DisplayName("Redis中存在版本号，返回对应版本")
        void whenVersionExists_thenReturnCachedVersion() {
            // given
            when(cachePort.get(any(), eq(Long.class))).thenReturn(Optional.of(7L));

            // when
            PermissionVersion version = permissionDomainService
                    .getCurrentPermissionVersion(USER_ID, USER_DOMAIN);

            // then
            assertThat(version.value()).isEqualTo(7L);
        }

        @Test
        @DisplayName("Redis中不存在版本号，返回初始版本号")
        void whenVersionNotExists_thenReturnInitialVersion() {
            // given
            when(cachePort.get(any(), eq(Long.class))).thenReturn(Optional.empty());

            // when
            PermissionVersion version = permissionDomainService
                    .getCurrentPermissionVersion(USER_ID, USER_DOMAIN);

            // then
            assertThat(version.value()).isEqualTo(PermissionVersion.initial().value());
        }

        @Test
        @DisplayName("Redis抛出异常，降级返回初始版本号")
        void whenRedisThrows_thenReturnInitialVersion() {
            // given
            when(cachePort.get(any(), any())).thenThrow(new RuntimeException("connection reset"));

            // when
            PermissionVersion version = permissionDomainService
                    .getCurrentPermissionVersion(USER_ID, USER_DOMAIN);

            // then
            assertThat(version.value()).isEqualTo(PermissionVersion.initial().value());
        }
    }

    // ================================================================
    // getCachedPermissions
    // ================================================================

    @Nested
    @DisplayName("getCachedPermissions()")
    class GetCachedPermissions {

        @Test
        @DisplayName("缓存命中，返回权限集合")
        void whenCacheHit_thenReturnPermissions() {
            // given
            Set<String> permissions = Set.of("member:read", "order:list");
            when(cachePort.get(any(), eq(Set.class))).thenReturn(Optional.of(permissions));

            // when
            Set<String> result = permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN);

            // then
            assertThat(result).containsExactlyInAnyOrder("member:read", "order:list");
        }

        @Test
        @DisplayName("缓存返回__EMPTY__占位符，过滤后返回空集合")
        void whenCacheHasPlaceholder_thenReturnEmptySet() {
            // given
            Set<String> placeholder = Collections.singleton("__EMPTY__");
            when(cachePort.get(any(), eq(Set.class))).thenReturn(Optional.of(placeholder));

            // when
            Set<String> result = permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("缓存未命中，返回空集合")
        void whenCacheMiss_thenReturnEmptySet() {
            // given
            when(cachePort.get(any(), eq(Set.class))).thenReturn(Optional.empty());

            // when
            Set<String> result = permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Redis抛出异常，降级返回空集合")
        void whenRedisThrows_thenReturnEmptySet() {
            // given
            when(cachePort.get(any(), any())).thenThrow(new RuntimeException("timeout"));

            // when
            Set<String> result = permissionDomainService.getCachedPermissions(USER_ID, USER_DOMAIN);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ================================================================
    // invalidatePermissionCache
    // ================================================================

    @Nested
    @DisplayName("invalidatePermissionCache()")
    class InvalidatePermissionCache {

        @Test
        @DisplayName("同时删除权限Key和摘要Key")
        void whenInvalidate_thenDeleteBothKeys() {
            // when
            permissionDomainService.invalidatePermissionCache(USER_ID, USER_DOMAIN);

            // then: 删除了两个key（权限 + 摘要）
            verify(cachePort, times(2)).delete(any());
        }

        @Test
        @DisplayName("Redis删除异常时，不抛出异常（静默失败）")
        void whenDeleteThrows_thenNoException() {
            // given
            doThrow(new RuntimeException("Redis error")).when(cachePort).delete(any());

            // when & then: 不抛出异常
            permissionDomainService.invalidatePermissionCache(USER_ID, USER_DOMAIN);
        }
    }

    // ================================================================
    // verifyPermissionDigest
    // ================================================================

    @Nested
    @DisplayName("verifyPermissionDigest()")
    class VerifyPermissionDigest {

        @Test
        @DisplayName("缓存中的摘要与当前权限匹配，返回true")
        void whenDigestMatches_thenReturnTrue() {
            // given: 缓存存储"member:read"的MD5
            Set<String> permissions = Set.of("member:read");
            String realDigest = PermissionDigest.from(permissions).value();
            when(cachePort.get(any(), eq(String.class))).thenReturn(Optional.of(realDigest));

            // when
            boolean result = permissionDomainService.verifyPermissionDigest(USER_ID, USER_DOMAIN, permissions);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("缓存中的摘要与当前权限不匹配，返回false")
        void whenDigestNotMatch_thenReturnFalse() {
            // given: 缓存存储一个假的摘要
            String fakeDigest = "00000000000000000000000000000000"; // 32位非真实MD5
            when(cachePort.get(any(), eq(String.class))).thenReturn(Optional.of(fakeDigest));

            // when
            boolean result = permissionDomainService.verifyPermissionDigest(
                    USER_ID, USER_DOMAIN, Set.of("member:read"));

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("缓存中无摘要，降级返回true（放行）")
        void whenNoDigestInCache_thenReturnTrue() {
            // given
            when(cachePort.get(any(), eq(String.class))).thenReturn(Optional.empty());

            // when
            boolean result = permissionDomainService.verifyPermissionDigest(
                    USER_ID, USER_DOMAIN, Set.of("member:read"));

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Redis抛出异常，降级返回true（放行）")
        void whenRedisThrows_thenReturnTrue() {
            // given
            when(cachePort.get(any(), any())).thenThrow(new RuntimeException("Redis error"));

            // when
            boolean result = permissionDomainService.verifyPermissionDigest(
                    USER_ID, USER_DOMAIN, Set.of("member:read"));

            // then
            assertThat(result).isTrue();
        }
    }
}
