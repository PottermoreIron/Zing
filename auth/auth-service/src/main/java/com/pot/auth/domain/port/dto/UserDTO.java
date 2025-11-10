package com.pot.auth.domain.port.dto;

import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户DTO（领域层）
 *
 * <p>这是领域层的DTO，与member-facade的DTO隔离
 * <p>由MemberModuleAdapter负责将facade的DTO转换成此DTO
 *
 * @author pot
 * @since 1.0.0
 */
@Builder
public record UserDTO(
        UserId userId,          // 使用Long而不是UserId，简化转换
        UserDomain userDomain,
        String username,
        String email,
        String phone,        // 统一字段名为phone
        String status,
        Set<String> permissions,  // 添加权限集合
        LocalDateTime emailVerifiedAt,
        LocalDateTime phoneVerifiedAt,
        LocalDateTime lastLoginAt,
        String lastLoginIp
) {
}
