package com.pot.auth.service.security;

import com.pot.member.facade.api.MemberFacade;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.security.core.userdetails.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 会员用户详情服务实现
 * <p>
 * 实现UserDetailsService，为Spring Security提供用户信息
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberFacade memberFacade;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("加载用户信息: username={}", username);

        // 尝试通过用户名查询
        R<MemberDTO> result = memberFacade.getMemberByUsername(username);

        MemberDTO member = null;
        if (result != null && result.isSuccess() && result.getData() != null) {
            member = result.getData();
        } else {
            // 尝试通过邮箱查询
            result = memberFacade.getMemberByEmail(username);
            if (result != null && result.isSuccess() && result.getData() != null) {
                member = result.getData();
            } else {
                // 尝试通过手机号查询
                result = memberFacade.getMemberByPhone(username);
                if (result != null && result.isSuccess() && result.getData() != null) {
                    member = result.getData();
                }
            }
        }

        if (member == null) {
            log.warn("用户不存在: username={}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 转换为SecurityUser
        return convertToSecurityUser(member);
    }

    /**
     * 根据用户ID加载用户
     */
    public UserDetails loadUserById(Long userId) {
        log.debug("根据ID加载用户信息: userId={}", userId);

        R<MemberDTO> result = memberFacade.getMemberById(userId);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            log.warn("用户不存在: userId={}", userId);
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }

        return convertToSecurityUser(result.getData());
    }

    /**
     * 转换MemberDTO为SecurityUser
     */
    private SecurityUser convertToSecurityUser(MemberDTO member) {
        // TODO: 从数据库或缓存中加载用户的角色和权限
        Set<String> roles = loadUserRoles(member.getMemberId());
        Set<String> permissions = loadUserPermissions(member.getMemberId());

        return SecurityUser.builder()
                .userId(member.getMemberId())
                .username(member.getNickname())
                .password(member.getPassword() != null ? member.getPassword() : "")
                .nickname(member.getNickname())
                .email(member.getEmail())
                .phone(member.getPhone())
                .avatarUrl(member.getAvatarUrl())
                .enabled(isAccountEnabled(member.getStatus()))
                .accountNonExpired(true)
                .accountNonLocked(isAccountNonLocked(member.getStatus()))
                .credentialsNonExpired(true)
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    /**
     * 加载用户角色
     * TODO: 实现从数据库加载角色的逻辑
     */
    private Set<String> loadUserRoles(Long userId) {
        // 这里先返回默认角色，后续需要从数据库查询
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        log.debug("加载用户角色: userId={}, roles={}", userId, roles);
        return roles;
    }

    /**
     * 加载用户权限
     * TODO: 实现从数据库加载权限的逻辑
     */
    private Set<String> loadUserPermissions(Long userId) {
        // 这里先返回默认权限，后续需要从数据库查询
        Set<String> permissions = new HashSet<>();
        permissions.add("user:read");
        permissions.add("user:update");
        log.debug("加载用户权限: userId={}, permissions={}", userId, permissions);
        return permissions;
    }

    /**
     * 判断账号是否启用
     */
    private boolean isAccountEnabled(Integer status) {
        // 1-正常 2-禁用 3-删除
        return status != null && status == 1;
    }

    /**
     * 判断账号是否未锁定
     */
    private boolean isAccountNonLocked(Integer status) {
        // 2-禁用
        return status != null && status != 2;
    }
}

