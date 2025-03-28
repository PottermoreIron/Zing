package com.pot.user.service.security.filter;

import com.pot.user.service.enums.LoginRegisterType;
import com.pot.user.service.security.token.CustomAuthenticationToken;
import com.pot.user.service.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/3/8 23:12
 * @description: 测试
 */
@Slf4j
public class CustomAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/login", "POST");

    public CustomAuthenticationFilter() {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER);
    }

    public CustomAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException {
        Map<String, Object> requestJson = HttpUtils.parseJsonRequest(request);
        String httpMethod = request.getMethod();
        int type = HttpUtils.obtainParamValue("type", requestJson, Integer.class);
        if (!httpMethod.equals("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        } else if (LoginRegisterType.getByCode(type) == null) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        } else {
            LoginRegisterType loginRegisterType = LoginRegisterType.getByCode(type);
            String identifier = HttpUtils.obtainParamValue(loginRegisterType.getIdentifier(), requestJson, String.class).trim();
            String credentials = HttpUtils.obtainParamValue(loginRegisterType.getCredentials(), requestJson, String.class).trim();
            CustomAuthenticationToken authRequest = CustomAuthenticationToken.unauthenticated(identifier, credentials, loginRegisterType);
            this.setDetails(request, authRequest);
            return this.getAuthenticationManager().authenticate(authRequest);
        }
    }

    protected void setDetails(HttpServletRequest request, CustomAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }
}
