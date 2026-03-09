package com.pot.auth.interfaces.controller;

import com.pot.auth.application.service.LogoutApplicationService;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.interfaces.dto.LogoutRequest;
import com.pot.zing.framework.common.model.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登出控制器
 *
 * <p>
 * 提供 Token 吊销能力，确保已登出用户的 Token 无法再被使用。
 *
 * <p>
 * 流程：
 * <ol>
 * <li>从 {@code Authorization} 请求头提取 AccessToken</li>
 * <li>将 AccessToken 加入 Redis 黑名单（设置与剩余有效期一致的 TTL）</li>
 * <li>若请求体提供了 RefreshToken，同步从 Redis 中删除其缓存</li>
 * </ol>
 *
 * <p>
 * 接口设计：
 * <ul>
 * <li>幂等：重复登出同一 Token 不会报错</li>
 * <li>容错：Token 已过期/无效时依然返回成功（防止客户端错误处理阻碍登出）</li>
 * </ul>
 *
 * <p>
 * 请求示例：
 * 
 * <pre>
 * POST /auth/api/v1/logout
 * Authorization: Bearer &lt;accessToken&gt;
 *
 * {
 *   "refreshToken": "xxx"   // 可选
 * }
 * </pre>
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LogoutController {

    private final LogoutApplicationService logoutApplicationService;

    /**
     * 登出（吊销当前 Token）
     *
     * <p>
     * 注意：网关会将 AccessToken 传递在 {@code Authorization: Bearer &lt;token&gt;} 头部，
     * 本接口直接读取此头部，无需在请求体中重复传递。
     */
    @PostMapping("/api/v1/logout")
    public R<Void> logout(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) LogoutRequest request) {

        String authorization = httpRequest.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            log.warn("[登出] 缺少 Authorization 请求头");
            return R.fail(AuthResultCode.TOKEN_INVALID);
        }

        // 兼容 "Bearer <token>" 和裸 token 两种格式
        String accessToken = authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim()
                : authorization.trim();

        String refreshToken = (request != null) ? request.refreshToken() : null;

        logoutApplicationService.logout(accessToken, refreshToken);

        return R.success(null);
    }
}
