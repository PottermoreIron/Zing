package com.pot.member.service.application.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record RoleDTO(
        Long roleId,
        String roleName,
        String roleCode,
        String description,
        Set<Long> permissionIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
