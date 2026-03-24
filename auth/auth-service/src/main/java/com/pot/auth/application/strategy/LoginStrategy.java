package com.pot.auth.application.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.interfaces.dto.auth.LoginRequest;

public interface LoginStrategy<T extends LoginRequest> {

    AuthenticationResult execute(AuthenticationContext context);

    boolean supports(LoginType loginType);
}