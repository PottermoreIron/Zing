package com.pot.auth.application.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.context.OneStopAuthContext;
import com.pot.auth.domain.shared.enums.AuthType;

public interface OneStopAuthStrategy {

    AuthenticationResult execute(OneStopAuthContext context);

    AuthType getSupportedAuthType();

    default boolean supports(AuthType authType) {
        return getSupportedAuthType() == authType;
    }
}