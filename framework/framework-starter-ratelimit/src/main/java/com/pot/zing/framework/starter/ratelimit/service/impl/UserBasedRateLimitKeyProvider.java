package com.pot.zing.framework.starter.ratelimit.service.impl;

import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import com.pot.zing.framework.starter.ratelimit.service.RateLimitKeyProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * @author: Pot
 * @created: 2025/10/18 22:40
 * @description: 自定义用户限流key提供者
 */
@Slf4j
public class UserBasedRateLimitKeyProvider implements RateLimitKeyProvider {

    /**
     * 用户ID请求头名称
     */
    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * 用户ID请求参数名称
     */
    private static final String USER_ID_PARAM = "userId";

    /**
     * 默认用户标识(未登录用户)
     */
    private static final String ANONYMOUS_USER = "anonymous";

    /**
     * 键分隔符
     */
    private static final String KEY_SEPARATOR = ":";

    @Override
    public String generateKey(String baseKey, ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String userId = extractUserId();
        return buildRateLimitKey(baseKey, userId);
    }

    @Override
    public RateLimitMethodEnum getSupportedType() {
        return RateLimitMethodEnum.USER_BASED;
    }

    /**
     * 提取用户ID
     * <p>
     * 优先级: Header > Parameter > Anonymous
     * </p>
     *
     * @return 用户ID,如果无法获取则返回匿名用户标识
     */
    protected String extractUserId() {
        return Optional.ofNullable(getCurrentRequest())
                .map(this::getUserIdFromRequest)
                .filter(StringUtils::hasText)
                .orElseGet(() -> {
                    log.debug("无法获取用户ID,使用匿名用户标识");
                    return ANONYMOUS_USER;
                });
    }

    /**
     * 获取当前HTTP请求
     *
     * @return HttpServletRequest,如果无法获取则返回null
     */
    protected HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.warn("获取当前请求失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从请求中提取用户ID
     *
     * @param request HTTP请求
     * @return 用户ID,如果未找到则返回null
     */
    protected String getUserIdFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 优先从Header获取
        String userId = request.getHeader(USER_ID_HEADER);
        if (StringUtils.hasText(userId)) {
            return userId;
        }

        // 其次从参数获取
        userId = request.getParameter(USER_ID_PARAM);
        return StringUtils.hasText(userId) ? userId : null;
    }

    /**
     * 构建限流键
     *
     * @param baseKey 基础键
     * @param userId  用户ID
     * @return 完整的限流键
     */
    protected String buildRateLimitKey(String baseKey, String userId) {
        return baseKey + KEY_SEPARATOR + "user" + KEY_SEPARATOR + userId;
    }
}