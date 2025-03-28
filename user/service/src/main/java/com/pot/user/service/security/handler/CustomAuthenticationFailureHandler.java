package com.pot.user.service.security.handler;

import com.pot.common.R;
import com.pot.common.enums.ResultCode;
import com.pot.user.service.utils.JacksonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author: Pot
 * @created: 2025/3/16 22:52
 * @description: 手机验证码认证失败处理器
 */
@Slf4j
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        log.info("exception:{}", exception.getMessage());
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        ResultCode resultCode = ResultCode.getError(exception.getMessage());
        R<Void> r = R.fail(resultCode);
        String json = JacksonUtils.toJson(r);
        response.getWriter().write(json);
    }
}
