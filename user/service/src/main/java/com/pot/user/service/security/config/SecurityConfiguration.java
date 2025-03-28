package com.pot.user.service.security.config;

import com.pot.common.R;
import com.pot.common.enums.ResultCode;
import com.pot.user.service.utils.JacksonUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

/**
 * @author: Pot
 * @created: 2025/3/8 23:43
 * @description: 测试
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .with(CustomSecurityConfigurer.customSecurityConfigurer(), Customizer.withDefaults())
                .csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login**", "/user/register", "/user/send/sms/code", "/user/test").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> setResponse(response, ResultCode.AUTHENTICATION_FAILED, HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> setResponse(response, ResultCode.USER_NO_PERMISSION, HttpStatus.FORBIDDEN)))
        ;
        return http.build();
    }

    private void setResponse(HttpServletResponse response, ResultCode resultCode, HttpStatus status) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status.value());
        R<Void> r = R.fail(resultCode);
        String json = JacksonUtils.toJson(r);
        response.getWriter().write(json);
    }
}
