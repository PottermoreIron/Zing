package com.pot.member.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 权限DTO
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDTO {

    private Long permissionId;
    private String permissionCode;
    private String permissionName;
    private String resource;
    private String action;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
