package com.pot.member.facade.dto;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

@Builder
public record RoleDTO(
        Long roleId,
        String roleCode,
        String roleName,
        String description) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
