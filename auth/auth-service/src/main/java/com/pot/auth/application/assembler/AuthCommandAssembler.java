package com.pot.auth.application.assembler;

import com.pot.auth.application.command.LoginCommand;
import com.pot.auth.application.command.LoginRequestCommand;
import com.pot.auth.application.command.OneStopAuthCommand;
import com.pot.auth.application.command.OneStopAuthRequestCommand;
import com.pot.auth.application.command.RegisterCommand;
import com.pot.auth.application.command.RegisterRequestCommand;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest;
import com.pot.auth.interfaces.dto.register.RegisterRequest;
import org.springframework.stereotype.Component;

/**
 * Converts transport-layer auth requests into application commands.
 */
@Component
public class AuthCommandAssembler {

    public LoginCommand toCommand(LoginRequest request) {
        return new LoginRequestCommand(
                request.loginType(),
                request.userDomain(),
                request.nickname(),
                request.email(),
                request.phone(),
                request.password(),
                request.verificationCode());
    }

    public RegisterCommand toCommand(RegisterRequest request) {
        return new RegisterRequestCommand(
                request.registerType(),
                request.userDomain(),
                request.nickname(),
                request.email(),
                request.phone(),
                request.password(),
                request.verificationCode(),
                request.code(),
                request.state(),
                request.oauth2ProviderCode());
    }

    public OneStopAuthCommand toCommand(OneStopAuthRequest request) {
        return new OneStopAuthRequestCommand(
                request.authType(),
                request.userDomain(),
                request.nickname(),
                request.email(),
                request.phone(),
                request.password(),
                request.verificationCode(),
                request.code(),
                request.state(),
                request.oauth2ProviderCode());
    }
}