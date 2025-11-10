package com.pot.zing.framework.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.zing.framework.security.handler.AccessDeniedHandlerImpl;
import com.pot.zing.framework.security.handler.AuthenticationEntryPointImpl;
import com.pot.zing.framework.security.handler.LogoutSuccessHandlerImpl;
import com.pot.zing.framework.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security自动配置类
 * <p>
 * 提供Spring Security的默认配置
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@EnableConfigurationProperties(SecurityProperties.class)
@ComponentScan("com.pot.zing.framework.security")
@ConditionalOnProperty(prefix = "zing.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SecurityAutoConfiguration {

    private final SecurityProperties securityProperties;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;
    private final LogoutSuccessHandlerImpl logoutSuccessHandler;

    /**
     * 配置Security过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF配置
                .csrf(csrf -> {
                    if (!securityProperties.isCsrfEnabled()) {
                        csrf.disable();
                    }
                })
                // 会话管理
                .sessionManagement(session -> {
                    if (securityProperties.getSessionStrategy() == SecurityProperties.SessionStrategy.STATELESS) {
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                    } else {
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
                    }
                })
                // 异常处理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // 授权配置
                .authorizeHttpRequests(authorize -> authorize
                        // 白名单放行
                        .requestMatchers(securityProperties.getWhitelist().toArray(new String[0])).permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )
                // 登出配置
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .clearAuthentication(true)
                )
                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * ObjectMapper（如果不存在）
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}


