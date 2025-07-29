package com.pot.user.service.security.config;

import com.pot.common.R;
import com.pot.common.enums.ResultCode;
import com.pot.user.service.utils.JacksonUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
@Slf4j
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 自定义登录页面
        http
                .csrf(CsrfConfigurer::disable)
                // 禁用表单项
                .formLogin(AbstractHttpConfigurer::disable)
                // 禁用HTTP Basic认证
                .httpBasic(AbstractHttpConfigurer::disable)
                // 使用无状态的Session管理，因为JWT令牌不需要服务器存储会话信息
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .with(CustomSecurityConfigurer.customSecurityConfigurer(), // 添加自定义的Security配置)
                        customizer -> {
                            // 这里可以进行其他的自定义配置
                            // 例如添加更多的过滤器或修改现有的配置
                            log.info("Custom Security Configuration applied.");
                        }
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/user/login", "/user/register", "/user/send/code", "/user/test", "/error", "/user/oauth2/**", "/api/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> setResponse(response, authException, HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> setResponse(response, accessDeniedException, HttpStatus.FORBIDDEN)))
        ;
        return http.build();
    }

    private void setResponse(HttpServletResponse response, Exception exception, HttpStatus status) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status.value());
        R<Void> r = R.fail(status == HttpStatus.UNAUTHORIZED ? ResultCode.AUTHENTICATION_FAILED : ResultCode.USER_NO_PERMISSION, exception.getMessage());
        String json = JacksonUtils.toJson(r);
        response.getWriter().write(json);
    }
}
