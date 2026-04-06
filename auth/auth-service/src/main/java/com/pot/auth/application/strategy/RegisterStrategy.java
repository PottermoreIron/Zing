package com.pot.auth.application.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.context.RegistrationContext;
import com.pot.auth.domain.shared.enums.RegisterType;

public interface RegisterStrategy {

    AuthenticationResult execute(RegistrationContext context);

    RegisterType getSupportedRegisterType();

    default boolean supports(RegisterType registerType) {
        return getSupportedRegisterType().equals(registerType);
    }
}