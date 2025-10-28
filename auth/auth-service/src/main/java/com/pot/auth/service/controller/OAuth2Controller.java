package com.pot.auth.service.controller;

import com.pot.auth.service.dto.request.login.OAuth2LoginRequest;
import com.pot.auth.service.dto.response.AuthResponse;
import com.pot.auth.service.enums.OAuth2Provider;
import com.pot.auth.service.oauth2.OAuth2ClientService;
import com.pot.auth.service.oauth2.factory.OAuth2ClientFactory;
import com.pot.auth.service.service.LoginService;
import com.pot.auth.service.strategy.factory.LoginStrategyFactory;
import com.pot.auth.service.strategy.impl.login.AbstractOAuth2LoginStrategy;
import com.pot.zing.framework.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/10/22
 * @description: OAuth2认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth/oauth2")
@RequiredArgsConstructor
@Validated
@Tag(name = "OAuth2认证", description = "第三方OAuth2登录相关接口（GitHub、Google等）")
public class OAuth2Controller {

    private final OAuth2ClientFactory oauth2ClientFactory;
    private final LoginService loginService;
    private final LoginStrategyFactory loginStrategyFactory;

    @GetMapping("/authorization-url/{provider}")
    @Operation(summary = "获取OAuth2授权URL", description = "获取指定提供商的OAuth2授权URL，用于跳转到第三方授权页面")
    public R<Map<String, String>> getAuthorizationUrl(
            @PathVariable
            @Parameter(description = "OAuth2提供商", example = "github")
            String provider) {

        log.info("获取OAuth2授权URL: provider={}", provider);

        // 1. 解析提供商
        OAuth2Provider oauth2Provider = OAuth2Provider.fromProvider(provider);

        // 2. 获取OAuth2客户端
        OAuth2ClientService oauth2Client = oauth2ClientFactory.getClientService(oauth2Provider);

        // 3. 生成state参数（通过对应的登录策略）
        AbstractOAuth2LoginStrategy loginStrategy = (AbstractOAuth2LoginStrategy)
                loginStrategyFactory.getStrategy(oauth2Provider.getLoginType());
        String state = loginStrategy.generateAndCacheState();

        // 4. 生成授权URL
        String authorizationUrl = oauth2Client.getAuthorizationUrl(state);

        Map<String, String> result = new HashMap<>();
        result.put("authorizationUrl", authorizationUrl);
        result.put("state", state);
        result.put("provider", provider);

        log.info("OAuth2授权URL生成成功: provider={}, state={}", provider, state);

        return R.success(result);
    }

    @PostMapping("/callback/{provider}")
    @Operation(summary = "OAuth2回调处理", description = "处理OAuth2授权回调，完成登录流程")
    public R<AuthResponse> handleCallback(
            @PathVariable
            @Parameter(description = "OAuth2提供商", example = "github")
            String provider,

            @RequestParam
            @NotBlank(message = "授权码不能为空")
            @Parameter(description = "OAuth2授权码", example = "4/0AY0e-g7...")
            String code,

            @RequestParam
            @NotBlank(message = "state参数不能为空")
            @Parameter(description = "防CSRF攻击的state参数", example = "random_state_string")
            String state) {

        log.info("OAuth2回调处理: provider={}, state={}", provider, state);

        // 1. 解析提供商
        OAuth2Provider oauth2Provider = OAuth2Provider.fromProvider(provider);

        // 2. 构建登录请求
        OAuth2LoginRequest loginRequest = new OAuth2LoginRequest();
        loginRequest.setType(oauth2Provider.getLoginType());
        loginRequest.setCode(code);
        loginRequest.setState(state);
        loginRequest.setProvider(provider);

        // 3. 执行登录
        AuthResponse response = loginService.login(loginRequest);

        log.info("OAuth2登录成功: provider={}, memberId={}",
                provider,
                response.getUserInfo().getMemberId());

        return R.success(response);
    }

    @GetMapping("/providers")
    @Operation(summary = "获取支持的OAuth2提供商列表", description = "获取系统支持的所有OAuth2提供商")
    public R<Map<String, String>> getSupportedProviders() {
        Map<String, String> providers = new HashMap<>();
        for (OAuth2Provider provider : OAuth2Provider.values()) {
            providers.put(provider.getProvider(), provider.getDisplayName());
        }
        return R.success(providers);
    }
}

