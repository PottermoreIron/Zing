package com.pot.auth.application.service;

import com.pot.auth.application.assembler.AuthCommandAssembler;
import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.strategy.OneStopAuthStrategy;
import com.pot.auth.application.validation.ValidationChain;
import com.pot.auth.application.strategy.factory.OneStopAuthStrategyFactory;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.context.OneStopAuthContext;
import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest;
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
        private final AuthCommandAssembler authCommandAssembler;

        /**
         * Executes a one-stop authentication request.
         */
        public OneStopAuthResponse authenticate(
                        OneStopAuthRequest request,
                        String ipAddress,
                        String userAgent) {

                log.info("[一键认证服务] 开始处理认证请求: authType={}, userDomain={}",
                                request.authType(), request.userDomain());

                OneStopAuthContext context = OneStopAuthContext.builder()
                                .request(authCommandAssembler.toCommand(request))
                                .ipAddress(IpAddress.of(ipAddress))
                                .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                                .build();

                oneStopAuthValidationChain.validate(context);

                OneStopAuthStrategy strategy = strategyFactory.getStrategy(request.authType());
                AuthenticationResult result = strategy.execute(context);

                OneStopAuthResponse response = OneStopAuthResponse.from(result);

                log.info("[一键认证服务] 认证成功: userId={}, authType={}",
                                result.userId(), request.authType());

                return response;
        }
}
