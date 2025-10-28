package com.pot.auth.service.controller.v1;

import com.pot.auth.service.dto.v1.request.CreateSessionRequest;
import com.pot.auth.service.dto.v1.request.RefreshSessionRequest;
import com.pot.auth.service.dto.v1.session.AuthSession;
import com.pot.auth.service.service.v1.AuthenticationService;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.security.annotation.PreventResubmit;
import com.pot.zing.framework.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证会话管理控制器（核心Controller）
 * <p>
 * 职责：
 * 1. 创建认证会话（登录） - 统一入口，支持所有认证方式
 * 2. 刷新认证会话 - 延长会话有效期
 * 3. 销毁认证会话（登出） - 单点登出
 * 4. 查询会话信息 - 会话状态查询
 * 5. 多设备会话管理 - 查看、管理用户的所有设备
 * <p>
 * 核心设计理念：
 * - 以"会话（Session）"为核心资源
 * - 所有登录方式统一为"创建会话"操作
 * - 通过grantType区分不同的认证方式
 * - 完全符合RESTful规范和OAuth 2.0标准
 * <p>
 * URL设计：
 * - POST   /api/v1/auth/sessions              创建会话（登录）
 * - GET    /api/v1/auth/sessions/current      获取当前会话
 * - PUT    /api/v1/auth/sessions/current      刷新当前会话
 * - DELETE /api/v1/auth/sessions/current      销毁当前会话（登出）
 * - GET    /api/v1/auth/sessions              获取用户所有会话
 * - DELETE /api/v1/auth/sessions/{sessionId}  销毁指定会话
 * <p>
 * 扩展性设计：
 * 新增认证方式无需修改此Controller，只需：
 * 1. 创建新的XxxGrantRequest类
 * 2. 实现authenticate方法
 * 3. 在CreateSessionRequest的@JsonSubTypes中注册
 *
 * @author Pot
 * @since 2025-10-25
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/sessions")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "1. 认证会话管理",
        description = "Authentication Session Management - 统一的认证入口，支持密码、验证码、OAuth2等多种认证方式"
)
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * 创建认证会话（统一登录入口）
     * <p>
     * 这是系统的核心认证接口，所有登录方式都通过此接口完成。
     * <p>
     * 支持的认证方式（grantType）：
     * 1. password - 密码登录（用户名/邮箱/手机号 + 密码）
     * 2. sms_code - 短信验证码登录
     * 3. email_code - 邮箱验证码登录
     * 4. authorization_code - OAuth2授权码登录（GitHub、Google等）
     * 5. wechat_qrcode - 微信扫码登录
     * 6. refresh_token - 刷新令牌
     * <p>
     * 工作流程：
     * 1. 客户端根据grantType构造对应的请求体
     * 2. Jackson根据grantType反序列化为具体的Request类
     * 3. 调用Request的authenticate方法执行认证
     * 4. 返回包含accessToken和refreshToken的会话信息
     * <p>
     * 安全特性：
     * - 防重放攻击：使用@PreventResubmit注解
     * - 限流保护：每个IP 5次/分钟
     * - 审计日志：记录所有认证尝试
     * - 失败锁定：连续失败5次锁定账户30分钟
     */
    @PostMapping
    @PreventResubmit(interval = 3, message = "登录操作过于频繁，请稍后再试")
    @Operation(
            summary = "创建认证会话（登录）",
            description = """
                    **统一登录入口** - 支持多种认证方式
                    
                    根据grantType字段自动识别认证方式：
                    - `password`: 密码登录
                    - `sms_code`: 短信验证码登录
                    - `email_code`: 邮箱验证码登录
                    - `authorization_code`: OAuth2登录
                    - `wechat_qrcode`: 微信扫码登录
                    
                    **成功后返回**：
                    - accessToken: 访问令牌（有效期1小时）
                    - refreshToken: 刷新令牌（有效期7天）
                    - userInfo: 用户基本信息
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "认证请求，根据grantType字段自动映射到对应的请求类型",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateSessionRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "密码登录",
                                            value = """
                                                    {
                                                      "grantType": "password",
                                                      "username": "user@example.com",
                                                      "password": "Password123!",
                                                      "clientId": "web",
                                                      "rememberMe": true
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "短信验证码登录",
                                            value = """
                                                    {
                                                      "grantType": "sms_code",
                                                      "phone": "13800138000",
                                                      "code": "123456",
                                                      "clientId": "android",
                                                      "autoRegister": true
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "OAuth2登录",
                                            value = """
                                                    {
                                                      "grantType": "authorization_code",
                                                      "provider": "github",
                                                      "code": "4/0AY0e-g7...",
                                                      "state": "random_state_string",
                                                      "clientId": "web",
                                                      "autoBind": true
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "微信扫码登录",
                                            value = """
                                                    {
                                                      "grantType": "wechat_qrcode",
                                                      "code": "wx_code_xxx",
                                                      "state": "random_state",
                                                      "clientId": "web"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "认证成功",
                            content = @Content(schema = @Schema(implementation = AuthSession.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "请求参数错误"),
                    @ApiResponse(responseCode = "401", description = "认证失败（用户名或密码错误、验证码错误等）"),
                    @ApiResponse(responseCode = "429", description = "请求过于频繁"),
                    @ApiResponse(responseCode = "500", description = "服务器内部错误")
            }
    )
    public R<AuthSession> createSession(
            @Valid @RequestBody
            @Parameter(hidden = true)  // 在Swagger中隐藏，使用@RequestBody的examples
            CreateSessionRequest request
    ) {
        log.info("创建认证会话请求: grantType={}, clientId={}",
                request.getGrantType(), request.getClientId());

        try {
            // 执行认证（多态调用，自动路由到对应的认证策略）
            AuthSession session = request.authenticate(authenticationService);

            log.info("认证会话创建成功: sessionId={}, userId={}, grantType={}",
                    session.getSessionId(),
                    session.getUserInfo().getUserId(),
                    request.getGrantType());

            return R.success(session, "登录成功");

        } catch (Exception e) {
            log.error("认证会话创建失败: grantType={}, error={}",
                    request.getGrantType(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取当前会话信息
     * <p>
     * 获取当前已认证用户的会话详细信息
     * 需要在请求头中携带有效的accessToken
     */
    @GetMapping("/current")
    @Operation(
            summary = "获取当前会话信息",
            description = "获取当前已认证用户的会话详细信息，包括会话ID、令牌信息、用户信息、设备信息等"
    )
    public R<AuthSession> getCurrentSession() {
        String sessionId = SecurityUtils.getCurrentSessionId();
        log.info("获取当前会话信息: sessionId={}", sessionId);

        AuthSession session = authenticationService.getSession(sessionId);
        return R.success(session);
    }

    /**
     * 刷新当前会话
     * <p>
     * 使用refreshToken获取新的accessToken和refreshToken
     * 用于延长会话有效期，避免用户重新登录
     */
    @PutMapping("/current")
    @Operation(
            summary = "刷新当前会话",
            description = """
                    使用refreshToken获取新的accessToken和refreshToken
                    
                    **使用场景**：
                    - accessToken即将过期或已过期
                    - 需要延长会话有效期
                    
                    **注意事项**：
                    - 旧的accessToken将立即失效
                    - 旧的refreshToken在一定时间内仍可使用（滚动刷新策略）
                    """
    )
    public R<AuthSession> refreshCurrentSession(
            @RequestBody @Valid RefreshSessionRequest request
    ) {
        String sessionId = SecurityUtils.getCurrentSessionId();
        log.info("刷新当前会话: sessionId={}", sessionId);

        AuthSession session = authenticationService.refreshSession(
                sessionId, request.getRefreshToken());

        log.info("会话刷新成功: sessionId={}, userId={}",
                session.getSessionId(), session.getUserInfo().getUserId());

        return R.success(session, "会话刷新成功");
    }

    /**
     * 销毁当前会话（登出）
     * <p>
     * 注销当前用户会话，使accessToken和refreshToken失效
     * 实现单点登出功能
     */
    @DeleteMapping("/current")
    @Operation(
            summary = "销毁当前会话（登出）",
            description = """
                    注销当前用户会话，使所有令牌失效
                    
                    **操作内容**：
                    1. 将accessToken加入黑名单
                    2. 将refreshToken加入黑名单
                    3. 清除Redis中的会话缓存
                    4. 记录登出审计日志
                    
                    **注意**：此操作不可逆
                    """
    )
    public R<Void> destroyCurrentSession() {
        String sessionId = SecurityUtils.getCurrentSessionId();
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("销毁当前会话: sessionId={}, userId={}", sessionId, userId);

        authenticationService.destroySession(sessionId);

        log.info("会话已销毁: sessionId={}", sessionId);
        return R.success(null, "登出成功");
    }

    /**
     * 获取用户所有会话（多设备管理）
     * <p>
     * 查询指定用户的所有活跃会话
     * 用于实现多设备管理功能
     */
    @GetMapping
    @Operation(
            summary = "获取用户所有会话",
            description = """
                    查询指定用户的所有活跃会话
                    
                    **使用场景**：
                    - 多设备管理
                    - 查看登录设备列表
                    - 安全审计
                    
                    **返回信息包括**：
                    - 会话ID
                    - 设备信息
                    - 登录时间
                    - 最后活跃时间
                    - 登录地点
                    """
    )
    public R<List<AuthSession>> listUserSessions(
            @RequestParam
            @Parameter(description = "用户ID", required = true, example = "123456")
            Long userId
    ) {
        log.info("获取用户所有会话: userId={}", userId);

        List<AuthSession> sessions = authenticationService.listUserSessions(userId);

        log.info("查询到{}个活跃会话: userId={}", sessions.size(), userId);
        return R.success(sessions);
    }

    /**
     * 销毁指定会话（踢出设备）
     * <p>
     * 强制下线指定的会话
     * 用于实现"踢出设备"功能
     */
    @DeleteMapping("/{sessionId}")
    @Operation(
            summary = "销毁指定会话",
            description = """
                    强制下线指定的会话（踢出设备）
                    
                    **使用场景**：
                    - 发现异常登录，强制下线
                    - 用户主动踢出其他设备
                    - 管理员强制下线用户
                    
                    **权限要求**：
                    - 普通用户只能踢出自己的其他设备
                    - 管理员可以踢出任何用户的会话
                    """
    )
    public R<Void> destroySession(
            @PathVariable
            @Parameter(description = "会话ID", required = true)
            String sessionId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("销毁指定会话: sessionId={}, operatorUserId={}", sessionId, currentUserId);

        authenticationService.forceLogoutSession(currentUserId, sessionId);

        log.info("会话已被销毁: sessionId={}", sessionId);
        return R.success(null, "设备已下线");
    }
}

