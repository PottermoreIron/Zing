package com.pot.auth.infrastructure.adapter.security;

import com.pot.auth.domain.port.SecurityPort;
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
 * Spring Security端口适配器
 *
 * <p>
 * 将Spring Security的认证信息适配为领域层的SecurityPort接口
 *
 * <p>
 * 设计模式：适配器模式（Adapter Pattern）
 * <ul>
 * <li>Target: SecurityPort接口</li>
 * <li>Adaptee: Spring Security的Authentication对象</li>
 * <li>Adapter: 本类负责转换</li>
 * </ul>
 *
 * <p>
 * 数据获取策略（多级降级）：
 * 
 * <pre>
 * 1. 优先从Authentication.getDetails()获取（推荐在登录时设置完整信息）
 * 2. 降级从GrantedAuthority获取（Spring Security标准方式）
 * 3. 兜底返回空集合（避免NPE）
 * </pre>
 *
 * <p>
 * 推荐用法：在UserDetailsService或登录成功处理器中设置Details：
 * 
 * <pre>{@code
 * Map<String, Object> details = new HashMap<>();
 * details.put("userId", user.getId());
 * details.put("permissions", user.getPermissions());
 * details.put("roles", user.getRoles());
 * authentication.setDetails(details);
 * }</pre>
 *
 * @author pot
 * @since 2025-12-14
 */
@Component
public class SpringSecurityPortAdapter implements SecurityPort {

    @Override
    public String getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // 优先从details获取
        Object details = authentication.getDetails();
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> detailsMap = (Map<String, Object>) details;
            Object userId = detailsMap.get("userId");
            if (userId != null) {
                return userId.toString();
            }
        }

        // 降级：使用用户名作为userId
        return authentication.getName();
    }

    @Override
    public Set<String> getCurrentUserPermissions() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptySet();
        }

        // 优先从details获取
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

        // 降级：从GrantedAuthority中提取permission（以PERM_开头）
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("PERM_"))
                .map(authority -> authority.substring(5)) // 去掉PERM_前缀
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getCurrentUserRoles() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptySet();
        }

        // 优先从details获取
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

        // 降级：从GrantedAuthority中提取role（以ROLE_开头）
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

        // 兜底：返回基础信息
        Map<String, Object> basicDetails = new HashMap<>();
        basicDetails.put("username", authentication.getName());
        basicDetails.put("authenticated", authentication.isAuthenticated());
        return basicDetails;
    }

    @Override
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 获取当前Authentication对象
     *
     * <p>
     * 从SecurityContextHolder中获取，线程安全
     *
     * @return Authentication对象，可能为null
     */
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
