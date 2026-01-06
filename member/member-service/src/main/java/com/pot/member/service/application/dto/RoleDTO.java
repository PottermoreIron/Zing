package com.pot.member.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 角色DTO
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {

    private Long roleId;
    private String roleName;
    private String roleCode;
    private String description;
    private Set<Long> permissionIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
