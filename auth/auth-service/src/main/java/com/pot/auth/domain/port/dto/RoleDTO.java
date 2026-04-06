package com.pot.auth.domain.port.dto;

import lombok.Builder;

@Builder
public record RoleDTO(
        Long roleId,
        String roleCode,
        String roleName,
        String roleDescription,
        Integer roleLevel
) {
}

