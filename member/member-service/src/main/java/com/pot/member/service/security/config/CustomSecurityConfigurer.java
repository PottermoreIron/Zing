package com.pot.member.service.security.config;

import com.pot.common.utils.JwtUtils;
import com.pot.member.service.security.filter.CustomAuthenticationFilter;
import com.pot.member.service.security.filter.JwtAuthenticationFilter;
import com.pot.member.service.utils.CommonUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * @author: Pot
 * @created: 2025/3/9 00:35
 * @description: 测试
 */
public class CustomSecurityConfigurer extends AbstractHttpConfigurer<CustomSecurityConfigurer, HttpSecurity> {

    private final CommonUtils commonUtils;
    private final JwtUtils jwtUtils;

    public CustomSecurityConfigurer(CommonUtils commonUtils, JwtUtils jwtUtils) {
        this.commonUtils = commonUtils;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void configure(HttpSecurity http) {
        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);
        http.addFilterBefore(new CustomAuthenticationFilter(authenticationManager, commonUtils), BasicAuthenticationFilter.class);
        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);
    }

    public static CustomSecurityConfigurer customSecurityConfigurer(CommonUtils commonUtils, JwtUtils jwtUtils) {
        return new CustomSecurityConfigurer(commonUtils, jwtUtils);
    }
}
