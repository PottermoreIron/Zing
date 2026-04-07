package com.pot.auth.application.command;

import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.valueobject.UserDomain;

/**
 * Application command built from a transport login request.
 */
public record LoginRequestCommand(
        LoginType loginType,
        UserDomain userDomain,
        String nickname,
        String email,
        String phone,
        String password,
        String verificationCode) implements LoginCommand {
}