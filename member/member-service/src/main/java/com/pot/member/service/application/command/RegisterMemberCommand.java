package com.pot.member.service.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册会员命令
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterMemberCommand {

    private String username;
    private String email;
    private String password;
    private String phoneNumber;
}
