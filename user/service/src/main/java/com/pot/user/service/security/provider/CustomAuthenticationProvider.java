package com.pot.user.service.security.provider;

import com.pot.user.service.enums.LoginRegisterType;
import com.pot.user.service.enums.SendCodeChannelType;
import com.pot.user.service.security.details.CustomUserDetailsService;
import com.pot.user.service.security.details.LoginUser;
import com.pot.user.service.security.token.CustomAuthenticationToken;
import com.pot.user.service.strategy.factory.VerificationCodeStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
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


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CustomAuthenticationToken authToken = (CustomAuthenticationToken) authentication;
        String identifier = authToken.getName();
        String credentials = authToken.getCredentials().toString();
        LoginRegisterType loginType = authToken.getLoginType();
        LoginUser loginUser = null;
        log.info("identifier={}, credentials={}, type={}", identifier, credentials, loginType);
        switch (loginType) {
            case PHONE_CODE ->
                    verificationCodeStrategyFactory.getStrategy(SendCodeChannelType.PHONE).validateCode(credentials, identifier);
            case USERNAME_PASSWORD -> {
                loginUser = (LoginUser) customUserDetailsService.loadUserByUsername(identifier);
                //todo 用户名密码登录
                throw new AuthenticationServiceException("Username and password login is not supported yet");
            }
            //todo 添加其他登录方式
            default -> throw new AuthenticationServiceException("Invalid login type: " + loginType);

        }
        // 验证码校验
        loginUser = (LoginUser) customUserDetailsService.loadUserByUsername(identifier);
        CustomAuthenticationToken customAuthenticationToken = new CustomAuthenticationToken(loginUser, loginUser.getAuthorities());
        customAuthenticationToken.setDetails(authentication.getDetails());
        return customAuthenticationToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CustomAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
