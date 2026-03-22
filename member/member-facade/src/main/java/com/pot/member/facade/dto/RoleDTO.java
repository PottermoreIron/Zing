package com.pot.member.facade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色 DTO（对外 RPC 契约）
 *
 * @author Pot
 * @since 2026-03-18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long roleId;
    private String roleCode;
    private String roleName;
    private String description;
}
