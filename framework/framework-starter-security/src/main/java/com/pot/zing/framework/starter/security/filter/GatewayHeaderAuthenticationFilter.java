package com.pot.zing.framework.starter.security.filter;

import com.pot.zing.framework.starter.security.port.PermissionLoaderPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads gateway-injected identity headers and populates the Spring Security context.
 *
 * <p>Downstream services trust the gateway to have already validated the JWT.
 * This filter translates the trusted headers into an {@code Authentication} object
 * so that {@code SecurityContextPort} and authorization aspects work transparently.</p>
 *
 * <p>If a {@link PermissionLoaderPort} bean is registered in the application context,
 * the filter will call it to load the user's permissions and store them in the
 * {@code Authentication} details map under the key {@code "permissions"}.</p>
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
@Slf4j
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID      = "X-User-Id";
    public static final String HEADER_USER_DOMAIN  = "X-User-Domain";
    public static final String HEADER_PERM_VERSION = "X-Perm-Version";
    public static final String HEADER_PERM_DIGEST  = "X-Perm-Digest";

    private final PermissionLoaderPort permissionLoader;

    public GatewayHeaderAuthenticationFilter() {
        this.permissionLoader = null;
    }

    public GatewayHeaderAuthenticationFilter(PermissionLoaderPort permissionLoader) {
        this.permissionLoader = permissionLoader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(HEADER_USER_ID);
        if (!StringUtils.hasText(userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userDomain  = request.getHeader(HEADER_USER_DOMAIN);
        String permVersion = request.getHeader(HEADER_PERM_VERSION);
        String permDigest  = request.getHeader(HEADER_PERM_DIGEST);

        Map<String, Object> details = new HashMap<>();
        details.put("userId", userId);
        putIfPresent(details, "userDomain",  userDomain);
        putIfPresent(details, "permVersion", permVersion);
        putIfPresent(details, "permDigest",  permDigest);

        if (permissionLoader != null) {
            try {
                Set<String> permissions = permissionLoader.loadPermissions(userId, userDomain, permVersion);
                details.put("permissions", permissions);
            } catch (Exception e) {
                log.warn("Failed to load permissions for user={} domain={}: {}", userId, userDomain, e.getMessage());
                details.put("permissions", Collections.emptySet());
            }
        }

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
