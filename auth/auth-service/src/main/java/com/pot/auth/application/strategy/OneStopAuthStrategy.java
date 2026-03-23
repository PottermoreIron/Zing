package com.pot.auth.application.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.context.OneStopAuthContext;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest;

public interface OneStopAuthStrategy<T extends OneStopAuthRequest> {

    AuthenticationResult execute(OneStopAuthContext context);

    boolean supports(AuthType authType);

    AuthType getSupportedAuthType();
}