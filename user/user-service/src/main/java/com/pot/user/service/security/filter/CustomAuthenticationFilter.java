package com.pot.user.service.security.filter;

import com.pot.common.R;
import com.pot.common.enums.ResultCode;
import com.pot.common.utils.HttpUtils;
import com.pot.common.utils.JacksonUtils;
import com.pot.user.service.controller.response.Tokens;
import com.pot.user.service.enums.LoginRegisterEnum;
import com.pot.user.service.security.details.LoginUser;
import com.pot.user.service.security.token.CustomAuthenticationToken;
import com.pot.user.service.utils.CommonUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
        } else if (LoginRegisterEnum.getByCode(type) == null) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        } else {
            LoginRegisterEnum loginRegisterEnum = LoginRegisterEnum.getByCode(type);
            String identifier = HttpUtils.obtainParamValue(loginRegisterEnum.getIdentifier(), requestJson, String.class).trim();
            String credentials = HttpUtils.obtainParamValue(loginRegisterEnum.getCredentials(), requestJson, String.class).trim();
            CustomAuthenticationToken authRequest = CustomAuthenticationToken.unauthenticated(identifier, credentials, loginRegisterEnum);
            this.setDetails(request, authRequest);
            return this.getAuthenticationManager().authenticate(authRequest);
        }
    }

    protected void setDetails(HttpServletRequest request, CustomAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException {
        LoginUser loginUser = (LoginUser) authResult.getPrincipal();
        Tokens tokens = CommonUtils.createAccessTokenAndRefreshToken(loginUser.getUser().getUid());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JacksonUtils.toJson(R.success(tokens)));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write(JacksonUtils.toJson(R.fail(ResultCode.AUTHENTICATION_FAILED, failed.getMessage())));
    }

}
