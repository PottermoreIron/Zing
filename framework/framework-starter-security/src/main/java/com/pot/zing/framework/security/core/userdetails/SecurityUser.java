package com.pot.zing.framework.security.core.userdetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security用户实体
 * <p>
 * 实现UserDetails��口，用于Spring Security认证和鉴权
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityUser implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 账号是否启用
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 账号是否未过期
     */
    @Builder.Default
    private boolean accountNonExpired = true;

    /**
     * 账号是否未锁定
     */
    @Builder.Default
    private boolean accountNonLocked = true;

    /**
     * 凭证是否未过期
     */
    @Builder.Default
    private boolean credentialsNonExpired = true;

    /**
     * 角色集合
     */
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    /**
     * 权限集合
     */
    @Builder.Default
    private Set<String> permissions = new HashSet<>();

    /**
     * 扩展信息
     */
    private transient Object extra;

    /**
     * 获取权限集合
     * <p>
     * 将角色和权限转换为GrantedAuthority集合
     * </p>
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 添加角色（带ROLE_前缀）
        authorities.addAll(roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet()));

        // 添加权限
        authorities.addAll(permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet()));

        return authorities;
    }

    /**
     * 检查是否拥有指定角色
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * 检查是否拥有指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /**
     * 检查是否拥有任意一个指定角色
     */
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否拥有任意一个指定权限
     */
    public boolean hasAnyPermission(String... permissions) {
        for (String permission : permissions) {
            if (this.permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }
}

