package com.pot.member.service.application.command;

import lombok.Builder;

@Builder
public record CreateMemberCommand(
        String nickname,
        String email,
        String password,
        String phoneNumber) {
}