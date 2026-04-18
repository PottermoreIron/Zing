package com.pot.zing.framework.starter.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads gateway-injected identity headers and populates the Spring Security context.
 *
 * <p>Downstream services trust the gateway to have already validated the JWT.
 * This filter translates the trusted headers into an {@code Authentication} object
 * so that {@code SecurityContextPort} and authorization aspects work transparently.</p>
 *
 * <p>Headers consumed:
 * <ul>
 *   <li>{@code X-User-Id} — required; request is treated as unauthenticated when absent</li>
 *   <li>{@code X-User-Domain} — optional user domain (e.g. member, admin)</li>
 *   <li>{@code X-Perm-Version} — optional permission cache version</li>
 *   <li>{@code X-Perm-Digest} — optional permission digest</li>
 * </ul>
 * </p>
 */
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID      = "X-User-Id";
    public static final String HEADER_USER_DOMAIN  = "X-User-Domain";
    public static final String HEADER_PERM_VERSION = "X-Perm-Version";
    public static final String HEADER_PERM_DIGEST  = "X-Perm-Digest";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(HEADER_USER_ID);
        if (!StringUtils.hasText(userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        Map<String, Object> details = new HashMap<>();
        details.put("userId", userId);
        putIfPresent(details, "userDomain",  request.getHeader(HEADER_USER_DOMAIN));
        putIfPresent(details, "permVersion", request.getHeader(HEADER_PERM_VERSION));
        putIfPresent(details, "permDigest",  request.getHeader(HEADER_PERM_DIGEST));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        authentication.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void putIfPresent(Map<String, Object> map, String key, String value) {
        if (StringUtils.hasText(value)) {
            map.put(key, value);
        }
    }
}
