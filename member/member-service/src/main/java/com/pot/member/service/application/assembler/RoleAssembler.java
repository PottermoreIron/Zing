package com.pot.member.service.application.assembler;

import com.pot.member.service.application.dto.RoleDTO;
import com.pot.member.service.domain.model.role.RoleAggregate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RoleAssembler {

        public RoleDTO toDTO(RoleAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }

        return RoleDTO.builder()
                .roleId(aggregate.getRoleId() != null ? aggregate.getRoleId().value() : null)
                .roleName(aggregate.getRoleName() != null ? aggregate.getRoleName().getValue() : null)
                .roleCode(aggregate.getRoleCode())
                .description(aggregate.getDescription())
                .permissionIds(aggregate.getPermissionIds())
                .createdAt(aggregate.getCreatedAt())
                .updatedAt(aggregate.getUpdatedAt())
                .build();
    }

        public List<RoleDTO> toDTOList(List<RoleAggregate> aggregates) {
        return aggregates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
