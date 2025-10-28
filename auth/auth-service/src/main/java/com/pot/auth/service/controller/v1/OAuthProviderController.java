package com.pot.auth.service.controller.v1;

import com.pot.auth.service.dto.v1.oAuth2.OAuthProviderInfo;
import com.pot.auth.service.dto.v1.request.OAuthCallbackRequest;
import com.pot.auth.service.dto.v1.response.AuthorizationUrlResponse;
import com.pot.auth.service.dto.v1.session.AuthSession;
import com.pot.auth.service.service.v1.OAuthProviderService;
import com.pot.zing.framework.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OAuth提供商控制器
 * <p>
 * 职责：
 * 1. 统一的OAuth 2.0流程管理
 * 2. 支持多个OAuth2提供商（GitHub、Google、WeChat、Facebook等）
 * 3. OAuth配置查询
 * 4. 授权URL生成
 * 5. 回调处理
 * <p>
 * URL设计：
 * - GET  /api/v1/auth/oauth/providers                             获取支持的提供商列表
 * - GET  /api/v1/auth/oauth/providers/{provider}                  获取提供商信息
 * - GET  /api/v1/auth/oauth/providers/{provider}/authorization-url 获取授权URL
 * - POST /api/v1/auth/oauth/callback                              OAuth回调处理（统一入口）
 * <p>
 * 扩展性设计：
 * 新增OAuth2提供商只需：
 * 1. 在配置文件中添加提供商配置
 * 2. 实现对应的OAuth2ClientService
 * 3. 无需修改Controller代码
 *
 * @author Pot
 * @since 2025-10-25
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "3. OAuth认证",
        description = "OAuth Provider Management - 统一的OAuth 2.0认证管理，支持GitHub、Google、WeChat等"
)
public class OAuthProviderController {

    private final OAuthProviderService oauthProviderService;

    /**
     * 获取支持的OAuth提供商列表
     */
    @GetMapping("/providers")
    @Operation(
            summary = "获取支持的OAuth提供商列表",
            description = """
                    获取系统支持的所有OAuth 2.0提供商
                    
                    **返回信息包括**：
                    - 提供商ID（github、google等）
                    - 提供商名称
                    - 是否启用
                    - 图标URL
                    - 授权范围
                    """
    )
    public R<List<OAuthProviderInfo>> listProviders() {
        log.info("获取OAuth提供商列表");
        List<OAuthProviderInfo> providers = oauthProviderService.listProviders();
        return R.success(providers);
    }

    /**
     * 获取提供商信息
     */
    @GetMapping("/providers/{provider}")
    @Operation(
            summary = "获取提供商详细信息",
            description = "获取指定OAuth提供商的详细配置信息（不包含敏感信息）"
    )
    public R<OAuthProviderInfo> getProviderInfo(
            @PathVariable
            @Parameter(description = "提供商ID", example = "github")
            String provider
    ) {
        log.info("获取OAuth提供商信息: provider={}", provider);
        OAuthProviderInfo info = oauthProviderService.getProviderInfo(provider);
        return R.success(info);
    }

    /**
     * 获取授权URL
     */
    @GetMapping("/providers/{provider}/authorization-url")
    @Operation(
            summary = "获取OAuth授权URL",
            description = """
                    获取指定提供商的OAuth授权URL
                    
                    **使用流程**：
                    1. 前端调用此接口获取授权URL和state
                    2. 前端跳转到授权URL
                    3. 用户在第三方平台授权
                    4. 第三方平台回调到redirectUri
                    5. 前端获取code和state
                    6. 前端调用回调接口完成登录
                    
                    **安全机制**：
                    - state参数用于防CSRF攻击
                    - state在Redis中缓存10分钟
                    - 回调时需验证state
                    """
    )
    public R<AuthorizationUrlResponse> getAuthorizationUrl(
            @PathVariable
            @Parameter(description = "提供商ID", example = "github")
            String provider,

            @RequestParam(required = false)
            @Parameter(description = "回调URI（可选，使用默认配置）", example = "https://example.com/oauth2/callback")
            String redirectUri
    ) {
        log.info("获取OAuth授权URL: provider={}, redirectUri={}", provider, redirectUri);

        AuthorizationUrlResponse response = oauthProviderService.getAuthorizationUrl(
                provider, redirectUri);

        log.info("授权URL生成成功: provider={}, state={}", provider, response.getState());
        return R.success(response);
    }

    /**
     * OAuth回调处理（统一入口）
     */
    @PostMapping("/callback")
    @Operation(
            summary = "OAuth回调处理",
            description = """
                    处理OAuth授权回调，完成登录流程
                    
                    **此接口是所有OAuth提供商的统一回调入口**
                    
                    **处理流程**：
                    1. 验证state参数（防CSRF）
                    2. 使用code换取access_token
                    3. 使用access_token获取用户信息
                    4. 查询或创建系统用户
                    5. 生成系统的accessToken和refreshToken
                    6. 返回会话信息
                    
                    **注意**：
                    - state必须与获取授权URL时返回的一致
                    - code有效期很短，通常只有10分钟
                    - 一个code只能使用一次
                    """
    )
    public R<AuthSession> handleOAuthCallback(
            @Valid @RequestBody OAuthCallbackRequest request
    ) {
        log.info("OAuth回调处理: provider={}, state={}", request.getProvider(), request.getState());

        AuthSession session = oauthProviderService.handleCallback(
                request.getProvider(),
                request.getCode(),
                request.getState()
        );

        log.info("OAuth回调处理成功: provider={}, userId={}",
                request.getProvider(), session.getUserInfo().getUserId());

        return R.success(session, "登录成功");
    }
}


