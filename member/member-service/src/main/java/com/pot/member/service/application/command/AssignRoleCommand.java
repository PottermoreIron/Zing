package com.pot.member.service.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleCommand {

    private Long memberId;
    private Long roleId;
    private String operator;
}
