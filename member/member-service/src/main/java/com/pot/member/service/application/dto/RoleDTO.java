package com.pot.member.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

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
