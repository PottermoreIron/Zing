package com.pot.user.service.security.provider;

import com.pot.user.service.enums.LoginRegisterType;
import com.pot.user.service.enums.SendCodeChannelType;
import com.pot.user.service.security.details.CustomUserDetailsService;
import com.pot.user.service.security.details.LoginUser;
import com.pot.user.service.security.token.CustomAuthenticationToken;
import com.pot.user.service.strategy.factory.VerificationCodeStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/2/25 23:17
 * @description: 测试spring security
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {


    private final CustomUserDetailsService customUserDetailsService;
    private final VerificationCodeStrategyFactory verificationCodeStrategyFactory;
    private final PasswordEncoder passwordEncoder;


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CustomAuthenticationToken authToken = (CustomAuthenticationToken) authentication;
        String identifier = authToken.getName();
        String credentials = authToken.getCredentials().toString();
        LoginRegisterType loginType = authToken.getLoginType();
        LoginUser loginUser = null;
        log.info("identifier={}, credentials={}, type={}", identifier, credentials, loginType);
        switch (loginType) {
            case USERNAME_PASSWORD, PHONE_PASSWORD, EMAIL_PASSWORD -> {
                loginUser = (LoginUser) customUserDetailsService.loadUserByIdentifier(identifier, loginType);
                if (loginUser == null) {
                    log.info("Not found user with {}: {}", loginType.getIdentifier(), identifier);
                    // 用户不存在，抛出异常
                    throw new AuthenticationServiceException("Not found user with %s".formatted(loginType.getIdentifier()));
                }
                String password = loginUser.getPassword();
                // 验证密码是否匹配
                if (StringUtils.isEmpty(password) || !passwordEncoder.matches(credentials, password)) {
                    log.info("Invalid {} or password for user: {}", loginType.getIdentifier(), identifier);
                    throw new AuthenticationServiceException("Invalid %s or password".formatted(loginType.getIdentifier()));
                }
            }
            case PHONE_CODE, EMAIL_CODE -> verificationCodeStrategyFactory.getStrategy(
                    loginType == LoginRegisterType.PHONE_CODE ? SendCodeChannelType.PHONE : SendCodeChannelType.EMAIL
            ).validateCode(credentials, identifier);

            default -> throw new AuthenticationServiceException("Unsupported login type: %s".formatted(loginType));

        }
        if (loginUser == null) {
            loginUser = (LoginUser) customUserDetailsService.loadUserByIdentifier(identifier, loginType);
        }
        CustomAuthenticationToken customAuthenticationToken = new CustomAuthenticationToken(loginUser, loginUser.getAuthorities());
        log.info("authenticate user={}", loginUser);
        customAuthenticationToken.setDetails(authentication.getDetails());
        return customAuthenticationToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CustomAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
