package com.pot.user.service.security.filter;

import com.pot.user.service.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = getToken(request);
        if (!StringUtils.isEmpty(token)) {
            Long uid = JwtUtils.getUid(token);
            // todo 权限控制
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(uid, null, null);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
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
