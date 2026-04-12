package com.pot.zing.framework.starter.authorization.aspect;

import com.pot.zing.framework.starter.authorization.annotation.RequireAnyPermission;
import com.pot.zing.framework.starter.authorization.annotation.RequirePermission;
import com.pot.zing.framework.starter.authorization.annotation.RequireRole;
import com.pot.zing.framework.starter.authorization.exception.PermissionDeniedException;
import com.pot.zing.framework.starter.authorization.expression.DefaultPermissionExpressionParser;
import com.pot.zing.framework.starter.authorization.security.AuthorizationSecurityAccessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AuthorizationAspect")
class AuthorizationAspectTest {

    @Test
    @DisplayName("Class-level @RequirePermission takes effect")
    void classLevelRequirePermission_appliesToMethod() {
        AuthorizationSecurityAccessor accessor = mockAccessor(true, Set.of("member:read"), Set.of("ROLE_USER"));
        ClassLevelPermissionService proxy = createPermissionProxy(new ClassLevelPermissionService(), accessor);

        assertThat(proxy.list()).isEqualTo("ok");
    }

    @Test
    @DisplayName("Method-level @RequirePermission overrides class-level configuration")
    void methodLevelRequirePermission_overridesClassLevel() {
        AuthorizationSecurityAccessor accessor = mockAccessor(true, Set.of("member:read"), Set.of("ROLE_USER"));
        MethodLevelPermissionService proxy = createPermissionProxy(new MethodLevelPermissionService(), accessor);

        assertThat(proxy.list()).isEqualTo("ok");
    }

    @Test
    @DisplayName("@RequireAnyPermission grants access when any one permission matches")
    void requireAnyPermission_allowsAnyMatchedPermission() {
        AuthorizationSecurityAccessor accessor = mockAccessor(true, Set.of("member:write"), Set.of("ROLE_USER"));
        AnyPermissionService proxy = createPermissionProxy(new AnyPermissionService(), accessor);

        assertThat(proxy.update()).isEqualTo("ok");
    }

    @Test
    @DisplayName("Class-level @RequireRole takes effect")
    void classLevelRequireRole_appliesToMethod() {
        AuthorizationSecurityAccessor accessor = mockAccessor(true, Set.of("member:read"), Set.of("ROLE_ADMIN"));
        ClassLevelRoleService proxy = createPermissionProxy(new ClassLevelRoleService(), accessor);

        assertThat(proxy.admin()).isEqualTo("ok");
    }

    @Test
    @DisplayName("Unauthenticated user is denied access immediately")
    void unauthenticatedUser_isRejected() {
        AuthorizationSecurityAccessor accessor = mockAccessor(false, Set.of(), Set.of());
        ClassLevelPermissionService proxy = createPermissionProxy(new ClassLevelPermissionService(), accessor);

        assertThatThrownBy(proxy::list)
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("Unauthenticated user");
    }

    @Test
    @DisplayName("SpEL permission expression can access method parameters")
    void spelPermission_canReadMethodParameter() {
        AuthorizationSecurityAccessor accessor = mockAccessor(true, Set.of("article:123:edit"), Set.of("ROLE_USER"));
        SpelPermissionService proxy = createPermissionProxy(new SpelPermissionService(), accessor);

        assertThat(proxy.edit(123L)).isEqualTo("ok");
    }

    private AuthorizationSecurityAccessor mockAccessor(boolean authenticated, Set<String> permissions, Set<String> roles) {
        AuthorizationSecurityAccessor accessor = mock(AuthorizationSecurityAccessor.class);
        when(accessor.isAuthenticated()).thenReturn(authenticated);
        when(accessor.getCurrentUserId()).thenReturn("1001");
        when(accessor.getCurrentUserPermissions()).thenReturn(permissions);
        when(accessor.getCurrentUserRoles()).thenReturn(roles);
        return accessor;
    }

    private <T> T createPermissionProxy(T target, AuthorizationSecurityAccessor accessor) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(new RequirePermissionAspect(new DefaultPermissionExpressionParser(), accessor));
        factory.addAspect(new RequireAnyPermissionAspect(new DefaultPermissionExpressionParser(), accessor));
        factory.addAspect(new RequireRoleAspect(accessor));
        return factory.getProxy();
    }

    @RequirePermission("member:read")
    static class ClassLevelPermissionService {

        String list() {
            return "ok";
        }
    }

    @RequirePermission("admin:read")
    static class MethodLevelPermissionService {

        @RequirePermission("member:read")
        String list() {
            return "ok";
        }
    }

    static class AnyPermissionService {

        @RequireAnyPermission({ "member:read", "member:write" })
        String update() {
            return "ok";
        }
    }

    @RequireRole("ROLE_ADMIN")
    static class ClassLevelRoleService {

        String admin() {
            return "ok";
        }
    }

    static class SpelPermissionService {

        @RequirePermission("hasPermission(#articleId, 'article', 'edit')")
        String edit(Long articleId) {
            return "ok";
        }
    }
}