package com.pot.member.service.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the {@code X-User-Id} header injected by the API gateway and exposes
 * it as a {@code memberId} request attribute so controllers can use
 * {@code @RequestAttribute("memberId")}.
 */
@Component
@Order(1)
public class UserContextFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String ATTR_MEMBER_ID = "memberId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader(HEADER_USER_ID);
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                request.setAttribute(ATTR_MEMBER_ID, Long.parseLong(userIdHeader));
            } catch (NumberFormatException ignored) {
                // Malformed header — leave attribute unset; controller will reject if required
            }
        }
        filterChain.doFilter(request, response);
    }
}
