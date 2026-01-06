package com.pot.member.service.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新会员资料命令
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberProfileCommand {

    private Long memberId;
    private String username;
    private String avatar;
    private String bio;
}
