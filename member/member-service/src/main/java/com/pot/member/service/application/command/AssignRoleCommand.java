package com.pot.member.service.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分配角色命令
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleCommand {

    private Long memberId;
    private Long roleId;
    private String operator;
}
