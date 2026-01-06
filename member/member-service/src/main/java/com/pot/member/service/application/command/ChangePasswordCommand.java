package com.pot.member.service.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改密码命令
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordCommand {

    private Long memberId;
    private String oldPassword;
    private String newPassword;
}
