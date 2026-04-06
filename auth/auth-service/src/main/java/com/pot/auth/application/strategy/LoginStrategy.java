package com.pot.auth.application.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.domain.shared.enums.LoginType;

public interface LoginStrategy {

    AuthenticationResult execute(AuthenticationContext context);

    LoginType getSupportedLoginType();

    default boolean supports(LoginType loginType) {
        return getSupportedLoginType().equals(loginType);
    }
}