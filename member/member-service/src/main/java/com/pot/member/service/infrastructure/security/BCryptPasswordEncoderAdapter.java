package com.pot.member.service.infrastructure.security;

import com.pot.member.service.domain.port.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt 密码编码适配器
 *
 * <p>
 * 实现领域层的 {@link PasswordEncoder} 端口，
 * 委托给 Spring Security 的 BCryptPasswordEncoder。
 * 领域层通过端口接口与 Spring Security 完全解耦。
 *
 * @author Pot
 * @since 2026-03-18
 */
@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoder {

    private final org.springframework.security.crypto.password.PasswordEncoder delegate = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
