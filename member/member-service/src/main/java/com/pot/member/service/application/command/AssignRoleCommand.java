package com.pot.member.service.application.command;

public record AssignRoleCommand(
        Long memberId,
        Long roleId,
        String operator) {
}
