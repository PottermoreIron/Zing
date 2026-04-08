package com.pot.member.service.application.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PermissionDTO(
        Long permissionId,
        String permissionCode,
        String permissionName,
        String resource,
        String action,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
