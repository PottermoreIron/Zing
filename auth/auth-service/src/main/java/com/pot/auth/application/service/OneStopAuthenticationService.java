package com.pot.auth.application.service;

import com.pot.auth.application.command.OneStopAuthCommand;
import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.strategy.OneStopAuthStrategy;
import com.pot.auth.application.validation.ValidationChain;
import com.pot.auth.application.strategy.factory.OneStopAuthStrategyFactory;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.context.OneStopAuthContext;
import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service for authenticate-or-register flows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OneStopAuthenticationService {

        private final OneStopAuthStrategyFactory strategyFactory;
        private final ValidationChain<OneStopAuthContext> oneStopAuthValidationChain;

        /**
         * Executes a one-stop authentication command.
         */
        public OneStopAuthResponse authenticate(
                        OneStopAuthCommand command,
                        String ipAddress,
                        String userAgent) {

                log.info("[OneStopAuth] Processing authentication request — authType={}, userDomain={}",
                                command.authType(), command.userDomain());

                OneStopAuthContext context = OneStopAuthContext.builder()
                                .request(command)
                                .ipAddress(IpAddress.of(ipAddress))
                                .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                                .build();

                oneStopAuthValidationChain.validate(context);

                OneStopAuthStrategy strategy = strategyFactory.getStrategy(command.authType());
                AuthenticationResult result = strategy.execute(context);

                OneStopAuthResponse response = OneStopAuthResponse.from(result);

                log.info("[OneStopAuth] Authentication successful — userId={}, authType={}",
                                result.userId(), command.authType());

                return response;
        }
}
