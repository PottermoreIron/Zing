package com.pot.auth.application.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.context.RegistrationContext;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.interfaces.dto.register.RegisterRequest;

public interface RegisterStrategy<T extends RegisterRequest> {

    AuthenticationResult execute(RegistrationContext context);

    boolean supports(RegisterType registerType);
}