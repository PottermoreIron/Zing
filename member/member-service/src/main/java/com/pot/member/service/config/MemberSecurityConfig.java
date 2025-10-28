package com.pot.member.service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Member Service Security配置
 * <p>
 * 会员服务的安全配置，启用方法级权限控制
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class MemberSecurityConfig {

    @Bean
    public SecurityFilterChain memberSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 对外提供的RPC接口（通过Feign调用），需要服务间认证
                        .requestMatchers("/member/**").permitAll()
                        // Actuator健康检查放行
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // 其他接口需要认证
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}

