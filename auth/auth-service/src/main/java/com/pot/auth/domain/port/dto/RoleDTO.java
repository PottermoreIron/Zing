package com.pot.auth.domain.port.dto;

import lombok.Builder;

/**
 * 角色DTO（领域层）
 *
 * @author pot
 * @since 1.0.0
 */
@Builder
public record RoleDTO(
        Long roleId,
        String roleCode,
        String roleName,
        String roleDescription,
        Integer roleLevel
) {
}

