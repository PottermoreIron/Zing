package com.pot.user.service.security.provider;

import com.pot.user.service.security.details.LoginUser;
import com.pot.user.service.security.details.SmsCodeUserDetailsService;
import com.pot.user.service.security.token.SmsCodeAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
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
public class SmsCodeAuthenticationProvider implements AuthenticationProvider {

    private final SmsCodeUserDetailsService smsCodeUserDetailsService;

    @Autowired
    public SmsCodeAuthenticationProvider(SmsCodeUserDetailsService smsCodeUserDetailsService) {
        this.smsCodeUserDetailsService = smsCodeUserDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String phone = authentication.getName();
        String code = authentication.getCredentials().toString();
        log.info("phone={}, code={}", phone, code);
        // todo 验证码校验
        LoginUser loginUser = (LoginUser) smsCodeUserDetailsService.loadUserByUsername(phone);
        SmsCodeAuthenticationToken smsCodeAuthenticationToken = new SmsCodeAuthenticationToken(loginUser, loginUser.getAuthorities());
        smsCodeAuthenticationToken.setDetails(authentication.getDetails());
        return smsCodeAuthenticationToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SmsCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
