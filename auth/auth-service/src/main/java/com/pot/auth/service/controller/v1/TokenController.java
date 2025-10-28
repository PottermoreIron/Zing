package com.pot.auth.service.controller.v1;

import com.pot.auth.service.dto.request.TokenRefreshRequest;
import com.pot.auth.service.dto.request.TokenRevokeRequest;
import com.pot.auth.service.dto.request.TokenValidateRequest;
import com.pot.auth.service.dto.response.TokenValidationResponse;
import com.pot.auth.service.dto.v1.session.AuthSession;
import com.pot.auth.service.service.v1.TokenService;
import com.pot.zing.framework.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * Token管理控制器
 * <p>
 * 提供Token的刷新、撤销、验证等功能
 *
 * @author Zing
 * @since 2025-10-25
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/tokens")
@RequiredArgsConstructor
@Validated
@Tag(name = "Token管理", description = "Token的刷新、撤销和验证接口")
public class TokenController {

    private final TokenService tokenService;

    /**
     * 刷新访问令牌
     * <p>
     * 使用Refresh Token换取新的Access Token
     *
     * @param request 刷新请求
     * @return 新的认证会话（包含新的access token）
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "刷新Token",
            description = "使用refresh token获取新的access token，延长会话有效期"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "刷新成功",
                    content = @Content(schema = @Schema(implementation = AuthSession.class))
            ),
            @ApiResponse(responseCode = "400", description = "Refresh Token无效或已过期"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    public R<AuthSession> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {

        log.info("[TokenController] 刷新Token, refreshToken={}",
                maskToken(request.getRefreshToken()));

        AuthSession session = tokenService.refreshToken(request);

        log.info("[TokenController] Token刷新成功, sessionId={}", session.getSessionId());
        return R.success(session, "Token刷新成功");
    }

    /**
     * 撤销令牌（登出或主动失效）
     * <p>
     * 将指定的Token加入黑名单，立即失效
     *
     * @param request 撤销请求
     * @return 操作结果
     */
    @PostMapping("/revoke")
    @Operation(
            summary = "撤销Token",
            description = "将指定的token加入黑名单，使其立即失效。可用于登出或主动废弃token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "撤销成功"),
            @ApiResponse(responseCode = "400", description = "Token格式错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    public R<Void> revokeToken(
            @Valid @RequestBody TokenRevokeRequest request) {

        log.info("[TokenController] 撤销Token, tokenType={}", request.getTokenType());

        tokenService.revokeToken(request);

        log.info("[TokenController] Token撤销成功");
        return R.success(null, "Token已撤销");
    }

    /**
     * 验证令牌有效性
     * <p>
     * 检查Token是否有效、是否在黑名单中、是否过期
     *
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/validate")
    @Operation(
            summary = "验证Token",
            description = "验证token的有效性，返回token状态和用户信息"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "验证完成（返回具体结果）",
                    content = @Content(schema = @Schema(implementation = TokenValidationResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Token格式错误")
    })
    public R<TokenValidationResponse> validateToken(
            @Valid @RequestBody TokenValidateRequest request) {

        log.debug("[TokenController] 验证Token");

        TokenValidationResponse response = tokenService.validateToken(request);

        return R.success(response, "验证完成");
    }

    /**
     * 批量撤销用户的所有Token
     * <p>
     * 用于强制用户下线所有设备
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/users/{userId}")
    @Operation(
            summary = "撤销用户所有Token",
            description = "强制用户在所有设备下线，撤销该用户的所有有效token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "撤销成功"),
            @ApiResponse(responseCode = "403", description = "无权限操作"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public R<Void> revokeUserTokens(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {

        log.info("[TokenController] 撤销用户所有Token, userId={}", userId);

        tokenService.revokeUserAllTokens(userId);

        log.info("[TokenController] 用户所有Token已撤销, userId={}", userId);
        return R.success(null, "用户所有Token已撤销");
    }

    /**
     * 获取Token的元信息
     *
     * @param token Access Token
     * @return Token元信息
     */
    @GetMapping("/introspect")
    @Operation(
            summary = "Token内省",
            description = "获取token的详细信息，包括过期时间、用户ID、客户端ID等"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "Token格式错误"),
            @ApiResponse(responseCode = "401", description = "Token无效")
    })
    public R<TokenValidationResponse> introspectToken(
            @Parameter(description = "Access Token", required = true)
            @RequestParam String token) {

        log.debug("[TokenController] Token内省");

        TokenValidateRequest request = new TokenValidateRequest();
        request.setToken(token);

        TokenValidationResponse response = tokenService.validateToken(request);

        return R.success(response, "查询成功");
    }

    /**
     * 脱敏Token（日志输出用）
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}

