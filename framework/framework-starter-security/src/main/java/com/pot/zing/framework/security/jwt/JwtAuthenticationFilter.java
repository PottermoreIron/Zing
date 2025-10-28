package com.pot.zing.framework.security.jwt;

import com.pot.zing.framework.security.config.SecurityProperties;
import com.pot.zing.framework.security.core.userdetails.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * <p>
 * 从请求中提取JWT Token，验证并设置到SecurityContext中
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenStore jwtTokenStore;
    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // 提取Token
            String token = extractToken(request);

            if (StringUtils.hasText(token)) {
                // 验证Token是否在黑名单中
                if (jwtTokenStore.isBlacklisted(token)) {
                    log.debug("Token在黑名单中: {}", token.substring(0, Math.min(20, token.length())));
                    filterChain.doFilter(request, response);
                    return;
                }

                // 验证Token有效性
                if (jwtTokenProvider.validateToken(token)) {
                    // 验证Token类型
                    if (jwtTokenProvider.validateTokenType(token, JwtTokenProvider.TokenType.ACCESS)) {
                        // 从Token中获取用户信息
                        SecurityUser user = jwtTokenProvider.getUserFromToken(token);

                        if (user != null) {
                            // 创建认证对象
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            user,
                                            null,
                                            user.getAuthorities()
                                    );
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            // 设置到SecurityContext
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            log.debug("JWT认证成功: userId={}, username={}", user.getUserId(), user.getUsername());
                        }
                    } else {
                        log.debug("Token类型不正确，期望ACCESS类型");
                    }
                }
            }
        } catch (Exception e) {
            log.error("JWT认证过滤器异常: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        // 从Header中获取
        String bearerToken = request.getHeader(securityProperties.getJwt().getHeader());
        String prefix = securityProperties.getJwt().getPrefix();

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(prefix)) {
            return bearerToken.substring(prefix.length());
        }

        // 从参数中获取（用于WebSocket等场景）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }

        return null;
    }
}