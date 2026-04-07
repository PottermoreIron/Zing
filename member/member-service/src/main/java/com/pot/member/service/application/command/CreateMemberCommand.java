package com.pot.member.service.application.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class CreateMemberCommand {

    private final String nickname;
    private final String email;
    private final String password;
    private final String phoneNumber;
}