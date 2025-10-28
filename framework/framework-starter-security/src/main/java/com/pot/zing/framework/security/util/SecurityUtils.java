package com.pot.zing.framework.security.util;

import com.pot.zing.framework.security.core.userdetails.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

/**
 * Security工具类
 * <p>
 * 提供安全相关的常用工具方法
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Slf4j
public class SecurityUtils {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 获取当前登录用户
     */
    public static Optional<SecurityUser> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
                return Optional.of((SecurityUser) authentication.getPrincipal());
            }
        } catch (Exception e) {
            log.error("获取当前用户失败", e);
        }
        return Optional.empty();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().map(SecurityUser::getUserId).orElse(null);
    }

    /**
     * 获取当前会话ID
     */
    public static String getCurrentSessionId() {
        return getCurrentUser().map(SecurityUser::getSessionId).orElse(null);
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        return getCurrentUser().map(SecurityUser::getUsername).orElse(null);
    }

    /**
     * 获取当前用户角色
     */
    public static Set<String> getCurrentUserRoles() {
        return getCurrentUser().map(SecurityUser::getRoles).orElse(Set.of());
    }

    /**
     * 获取当前用户权限
     */
    public static Set<String> getCurrentUserPermissions() {
        return getCurrentUser().map(SecurityUser::getPermissions).orElse(Set.of());
    }

    /**
     * 检查当前用户是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        return getCurrentUser().map(user -> user.hasRole(role)).orElse(false);
    }

    /**
     * 检查当前用户是否拥有指定权限
     */
    public static boolean hasPermission(String permission) {
        return getCurrentUser().map(user -> user.hasPermission(permission)).orElse(false);
    }

    /**
     * 检查当前用户是否拥有任意一个指定角色
     */
    public static boolean hasAnyRole(String... roles) {
        return getCurrentUser().map(user -> user.hasAnyRole(roles)).orElse(false);
    }

    /**
     * 检查当前用户是否拥有任意一个指定权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        return getCurrentUser().map(user -> user.hasAnyPermission(permissions)).orElse(false);
    }

    /**
     * 加密密码
     */
    public static String encodePassword(String rawPassword) {
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    /**
     * 验证密码
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }

    /**
     * 检查密码强度
     *
     * @param password 密码
     * @return true表示符合强度要求
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        // 至少包含一个大写字母、一个小写字母、一个数字
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        return hasUpperCase && hasLowerCase && hasDigit;
    }

    /**
     * 清除当前Security上下文
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 设置当前Security上下文
     */
    public static void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

