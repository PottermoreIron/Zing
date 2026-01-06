package com.pot.member.service.application.assembler;

import com.pot.member.service.application.dto.PermissionDTO;
import com.pot.member.service.domain.model.permission.PermissionAggregate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限装配器 - 负责聚合根与DTO之间的转换
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Component
public class PermissionAssembler {

    /**
     * 将聚合根转换为DTO
     */
    public PermissionDTO toDTO(PermissionAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }

        return PermissionDTO.builder()
                .permissionId(aggregate.getPermissionId() != null ? aggregate.getPermissionId().value() : null)
                .permissionCode(aggregate.getPermissionCode())
                .permissionName(aggregate.getPermissionName())
                .resource(aggregate.getResource())
                .action(aggregate.getAction())
                .description(aggregate.getDescription())
                .createdAt(aggregate.getCreatedAt())
                .updatedAt(aggregate.getUpdatedAt())
                .build();
    }

    /**
     * 批量转换
     */
    public List<PermissionDTO> toDTOList(List<PermissionAggregate> aggregates) {
        return aggregates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Set批量转换
     */
    public Set<PermissionDTO> toDTOSet(Set<PermissionAggregate> aggregates) {
        return aggregates.stream()
                .map(this::toDTO)
                .collect(Collectors.toSet());
    }
}
