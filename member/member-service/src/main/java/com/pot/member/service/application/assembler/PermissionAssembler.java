package com.pot.member.service.application.assembler;

import com.pot.member.service.application.dto.PermissionDTO;
import com.pot.member.service.domain.model.permission.PermissionAggregate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PermissionAssembler {

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

        public List<PermissionDTO> toDTOList(List<PermissionAggregate> aggregates) {
        return aggregates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

        public Set<PermissionDTO> toDTOSet(Set<PermissionAggregate> aggregates) {
        return aggregates.stream()
                .map(this::toDTO)
                .collect(Collectors.toSet());
    }
}
