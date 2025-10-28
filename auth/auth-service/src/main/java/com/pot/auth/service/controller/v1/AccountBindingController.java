package com.pot.auth.service.controller.v1;

import com.pot.auth.service.dto.request.BindAccountRequest;
import com.pot.auth.service.dto.request.UnbindAccountRequest;
import com.pot.auth.service.dto.response.AccountBindingInfo;
import com.pot.auth.service.service.v1.AccountBindingService;
import com.pot.zing.framework.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 账户绑定控制器
 * <p>
 * 管理用户与第三方OAuth账号的绑定关系
 * 支持：
 * - 绑定第三方账号（微信、GitHub、Google等）
 * - 解绑第三方账号
 * - 查看已绑定账号列表
 * - 切换主账号
 *
 * @author Zing
 * @since 2025-10-25
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/bindings")
@RequiredArgsConstructor
@Validated
@Tag(name = "账户绑定", description = "第三方账号绑定管理接口")
@SecurityRequirement(name = "Bearer Authentication")
public class AccountBindingController {

    private final AccountBindingService accountBindingService;

    /**
     * 绑定第三方账号
     * <p>
     * 将OAuth2提供商的账号绑定到当前用户
     * 使用场景：
     * - 用户在设置中绑定微信、GitHub等账号
     * - 绑定后可使用第三方账号登录
     *
     * @param request        绑定请求
     * @param authentication 当前认证信息
     * @return 绑定结果
     */
    @PostMapping
    @Operation(
            summary = "绑定第三方账号",
            description = "将OAuth2提供商的账号绑定到当前登录用户。需要先通过OAuth2授权流程获取code"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "绑定成功",
                    content = @Content(schema = @Schema(implementation = AccountBindingInfo.class))
            ),
            @ApiResponse(responseCode = "400", description = "参数错误或授权码无效"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "409", description = "该第三方账号已被绑定到其他用户")
    })
    public R<AccountBindingInfo> bindAccount(@Valid @RequestBody BindAccountRequest request, Authentication authentication) {

        Long userId = extractUserId(authentication);

        log.info("[AccountBindingController] 绑定第三方账号, userId={}, provider={}",
                userId, request.getProvider());

        AccountBindingInfo bindingInfo = accountBindingService.bindAccount(userId, request);

        log.info("[AccountBindingController] 绑定成功, userId={}, provider={}, openId={}",
                userId, bindingInfo.getProvider(), maskOpenId(bindingInfo.getOpenId()));

        return R.success(bindingInfo, "绑定成功");
    }

    /**
     * 解绑第三方账号
     * <p>
     * 解除当前用户与指定OAuth提供商的绑定
     * 注意：至少保留一种登录方式，不能解绑所有账号
     *
     * @param provider       OAuth提供商名称（如：wechat, github, google）
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @DeleteMapping("/{provider}")
    @Operation(
            summary = "解绑第三方账号",
            description = "解除与指定OAuth提供商的绑定关系。注意：至少需要保留一种登录方式"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "解绑成功"),
            @ApiResponse(responseCode = "400", description = "不能解绑最后一种登录方式"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "未找到该绑定关系")
    })
    public R<Void> unbindAccount(@Parameter(description = "OAuth提供商（如：wechat, github, google）", required = true)
                                 @PathVariable String provider,
                                 Authentication authentication) {

        Long userId = extractUserId(authentication);

        log.info("[AccountBindingController] 解绑第三方账号, userId={}, provider={}",
                userId, provider);

        UnbindAccountRequest request = UnbindAccountRequest.builder()
                .provider(provider)
                .build();

        accountBindingService.unbindAccount(userId, request);

        log.info("[AccountBindingController] 解绑成功, userId={}, provider={}", userId, provider);

        return R.success(null, "解绑成功");
    }

    /**
     * 获取当前用户的所有绑定账号
     * <p>
     * 返回用户已绑定的所有第三方账号列表
     *
     * @param authentication 当前认证信息
     * @return 绑定账号列表
     */
    @GetMapping
    @Operation(
            summary = "获取绑定列表",
            description = "获取当前用户已绑定的所有第三方账号"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AccountBindingInfo.class))
            ),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    public R<List<AccountBindingInfo>> listBindings(Authentication authentication) {

        Long userId = extractUserId(authentication);

        log.debug("[AccountBindingController] 获取绑定列表, userId={}", userId);

        List<AccountBindingInfo> bindings = accountBindingService.listBindings(userId);

        return R.success(bindings, "查询成功");
    }

    /**
     * 获取指定提供商的绑定信息
     *
     * @param provider       OAuth提供商名称
     * @param authentication 当前认证信息
     * @return 绑定信息
     */
    @GetMapping("/{provider}")
    @Operation(
            summary = "获取指定绑定信息",
            description = "获取当前用户在指定OAuth提供商的绑定详情"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AccountBindingInfo.class))
            ),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "未找到绑定关系")
    })
    public R<AccountBindingInfo> getBinding(
            @Parameter(description = "OAuth提供商", required = true)
            @PathVariable String provider,
            Authentication authentication) {

        Long userId = extractUserId(authentication);

        log.debug("[AccountBindingController] 获取绑定信息, userId={}, provider={}",
                userId, provider);

        AccountBindingInfo binding = accountBindingService.getBinding(userId, provider);

        return R.success(binding, "查询成功");
    }

    /**
     * 设置主账号
     * <p>
     * 将某个绑定账号设为主账号（优先显示的账号）
     *
     * @param provider       OAuth提供商名称
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @PutMapping("/{provider}/primary")
    @Operation(
            summary = "设置主账号",
            description = "将指定的第三方账号设置为主账号"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "未找到绑定关系")
    })
    public R<Void> setPrimaryAccount(
            @Parameter(description = "OAuth提供商", required = true)
            @PathVariable String provider,
            Authentication authentication) {

        Long userId = extractUserId(authentication);

        log.info("[AccountBindingController] 设置主账号, userId={}, provider={}",
                userId, provider);

        accountBindingService.setPrimaryAccount(userId, provider);

        log.info("[AccountBindingController] 设置成功");

        return R.success(null, "设置成功");
    }

    /**
     * 检查第三方账号是否已被绑定
     * <p>
     * 用于OAuth登录时判断是否需要绑定还是直接登录
     *
     * @param provider OAuth提供商
     * @param openId   第三方平台的用户ID
     * @return 检查结果
     */
    @GetMapping("/check")
    @Operation(
            summary = "检查绑定状态",
            description = "检查指定的第三方账号是否已被任何用户绑定"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    public R<Boolean> checkBinding(
            @Parameter(description = "OAuth提供商", required = true)
            @RequestParam String provider,

            @Parameter(description = "第三方平台的用户ID", required = true)
            @RequestParam String openId) {

        log.debug("[AccountBindingController] 检查绑定状态, provider={}, openId={}",
                provider, maskOpenId(openId));

        boolean isBound = accountBindingService.isAccountBound(provider, openId);

        return R.success(isBound, "查询成功");
    }

    /**
     * 刷新绑定账号信息
     * <p>
     * 从OAuth提供商重新获取用户信息并更新
     *
     * @param provider       OAuth提供商名称
     * @param authentication 当前认证信息
     * @return 更新后的绑定信息
     */
    @PostMapping("/{provider}/refresh")
    @Operation(
            summary = "刷新绑定信息",
            description = "从OAuth提供商重新获取用户信息并更新绑定数据"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刷新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "未找到绑定关系")
    })
    public R<AccountBindingInfo> refreshBinding(
            @Parameter(description = "OAuth提供商", required = true)
            @PathVariable String provider,
            Authentication authentication) {

        Long userId = extractUserId(authentication);

        log.info("[AccountBindingController] 刷新绑定信息, userId={}, provider={}",
                userId, provider);

        AccountBindingInfo binding = accountBindingService.refreshBinding(userId, provider);

        log.info("[AccountBindingController] 刷新成功");

        return R.success(binding, "刷新成功");
    }

    /**
     * 从Authentication中提取用户ID
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("未登录");
        }

        Object principal = authentication.getPrincipal();

        // 根据实际的Authentication实现提取userId
        if (principal instanceof Long) {
            return (Long) principal;
        }

        // 如果使用的是自定义的UserDetails，需要适配
        // 例如: return ((CustomUserDetails) principal).getUserId();

        throw new IllegalStateException("无法获取用户ID");
    }

    /**
     * 脱敏OpenID（日志输出用）
     */
    private String maskOpenId(String openId) {
        if (openId == null || openId.length() < 8) {
            return "***";
        }
        return openId.substring(0, 4) + "***" + openId.substring(openId.length() - 4);
    }
}

