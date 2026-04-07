package com.pot.auth.application.command;

import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;

/**
 * Application command built from a transport register request.
 */
public record RegisterRequestCommand(
        RegisterType registerType,
        UserDomain userDomain,
        String nickname,
        String email,
        String phone,
        String password,
        String verificationCode,
        String code,
        String state,
        String oauth2ProviderCode) implements RegisterCommand {
}