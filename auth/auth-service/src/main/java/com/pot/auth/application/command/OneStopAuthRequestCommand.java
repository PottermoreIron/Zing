package com.pot.auth.application.command;

import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;

/**
 * Application command built from a transport one-stop auth request.
 */
public record OneStopAuthRequestCommand(
                AuthType authType,
                UserDomain userDomain,
                String nickname,
                String email,
                String phone,
                String password,
                String verificationCode,
                String code,
                String state,
                String redirectUri,
                String oauth2ProviderCode) implements OneStopAuthCommand {
}