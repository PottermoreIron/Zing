package com.pot.member.service.security.filter;

import com.pot.common.utils.JwtUtils;
import com.pot.member.service.security.token.CustomAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @author: Pot
 * @created: 2025/3/23 17:03
 * @description: JWT验证Filter
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            Long uid = JwtUtils.getUid(request);
            CustomAuthenticationToken authenticationToken = new CustomAuthenticationToken(uid, null);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        } catch (Exception e) {
        }
        filterChain.doFilter(request, response);
    }

    private String getToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.isEmpty(header) || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }
}
