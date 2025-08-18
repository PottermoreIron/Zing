package com.pot.member.service.security.token;

import com.pot.member.service.enums.LoginRegisterEnum;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * @author: Pot
 * @created: 2025/2/25 23:14
 * @description: 测试spring security
 */
public class CustomAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal;
    private final Object credentials;
    @Getter
    private final LoginRegisterEnum loginType;

    public CustomAuthenticationToken(Object principal, Object credentials, LoginRegisterEnum loginType) {
        super(null);
        this.principal = principal;
        this.credentials = credentials;
        this.loginType = loginType;
        setAuthenticated(false);
    }

    public CustomAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        this.loginType = null;
        super.setAuthenticated(true);
    }

    public static CustomAuthenticationToken unauthenticated(Object principal, Object credentials, LoginRegisterEnum loginType) {
        return new CustomAuthenticationToken(principal, credentials, loginType);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
}

