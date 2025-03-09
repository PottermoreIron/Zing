package com.pot.user.service.security.config;

import com.pot.user.service.security.filter.SmsCodeAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * @author: Pot
 * @created: 2025/3/9 00:35
 * @description: 测试
 */
public class CustomSecurityConfigurer extends AbstractHttpConfigurer<CustomSecurityConfigurer, HttpSecurity> {

    @Override
    public void configure(HttpSecurity http) {
        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);
        http.addFilterBefore(new SmsCodeAuthenticationFilter(authenticationManager), BasicAuthenticationFilter.class);
    }

    public static CustomSecurityConfigurer customSecurityConfigurer() {
        return new CustomSecurityConfigurer();
    }
}
