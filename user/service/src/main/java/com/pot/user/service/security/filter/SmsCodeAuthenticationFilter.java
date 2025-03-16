package com.pot.user.service.security.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.user.service.security.token.SmsCodeAuthenticationToken;
import com.pot.user.service.utils.JacksonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: Pot
 * @created: 2025/3/8 23:12
 * @description: 测试
 */
@Slf4j
public class SmsCodeAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/login", "POST");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public SmsCodeAuthenticationFilter() {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER);
    }

    public SmsCodeAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException {
        Map<String, String> requestJson = parseJsonRequest(request);
        String type = requestJson.get("type");
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        } else if (!"mobile".equals(type)) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        } else {

            String phone = obtainPhone(requestJson);
            String code = obtainCode(requestJson);
            SmsCodeAuthenticationToken authRequest = SmsCodeAuthenticationToken.unauthenticated(phone, code);
            this.setDetails(request, authRequest);
            return this.getAuthenticationManager().authenticate(authRequest);
        }
    }

    protected void setDetails(HttpServletRequest request, SmsCodeAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }

    @Nullable
    protected String obtainPhone(Map<String, String> requestJson) {
        String phoneParameter = "phone";
        return Optional.ofNullable(requestJson.get(phoneParameter))
                .map(String::trim)
                .orElse("");
    }

    @Nullable
    protected String obtainCode(Map<String, String> requestJson) {
        String codedParameter = "code";
        return Optional.ofNullable(requestJson.get(codedParameter))
                .map(String::trim)
                .orElse("");
    }

    private Map<String, String> parseJsonRequest(HttpServletRequest request) throws IOException {
        // 使用 Jackson 解析请求体
        return JacksonUtils.toObject(request.getReader().lines().collect(Collectors.joining()), new TypeReference<>() {
        });
    }
}
