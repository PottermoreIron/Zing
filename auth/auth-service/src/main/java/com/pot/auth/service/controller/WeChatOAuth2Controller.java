package com.pot.auth.service.controller;

import com.pot.auth.service.dto.request.login.OAuth2LoginRequest;
import com.pot.auth.service.dto.response.AuthResponse;
import com.pot.auth.service.dto.response.AuthUserInfoVO;
import com.pot.auth.service.dto.wechat.WeChatQrCodeResponse;
import com.pot.auth.service.dto.wechat.WeChatScanStatusResponse;
import com.pot.auth.service.enums.LoginType;
import com.pot.auth.service.oauth2.wechat.WeChatQrCodeService;
import com.pot.auth.service.service.LoginService;
import com.pot.auth.service.strategy.impl.login.WeChatOAuth2LoginStrategy;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 微信OAuth2登录控制器
 * <p>
 * 提供微信扫码登录的完整功能：
 * 1. 生成扫码登录二维码
 * 2. 轮询扫码状态
 * 3. 处理微信授权回调
 */
@Slf4j
@RestController
@RequestMapping("/auth/wechat")
@RequiredArgsConstructor
@Validated
@Tag(name = "微信登录", description = "微信扫码登录相关接口")
@ConditionalOnProperty(prefix = "oauth2.clients.wechat", name = "enabled", havingValue = "true")
public class WeChatOAuth2Controller {

    private final WeChatQrCodeService qrCodeService;
    private final LoginService loginService;
    private final WeChatOAuth2LoginStrategy weChatLoginStrategy;

    /**
     * 获取微信扫码登录二维码
     * 前端接收到二维码URL后，使用qrcode.js等库生成二维码图片展示给用户
     */
    @GetMapping("/qrcode")
    @Operation(
            summary = "获取微信扫码登录二维码",
            description = "生成微信扫码登录二维码URL，前端需要使用该URL生成二维码图片供用户扫描"
    )
    @RateLimit(key = "wechat:qrcode", rate = 10)
    public R<WeChatQrCodeResponse> getQrCode() {
        log.info("请求生成微信扫码登录二维码");

        try {
            // 1. 生成state参数（防CSRF攻击）
            String state = weChatLoginStrategy.generateAndCacheState();

            // 2. 生成二维码URL
            WeChatQrCodeResponse response = qrCodeService.generateQrCode(state);

            log.info("微信扫码二维码生成成功: state={}, qrCodeId={}", state, response.getQrCodeId());

            return R.success(response);
        } catch (Exception e) {
            log.error("生成微信扫码二维码失败", e);
            return R.fail("生成二维码失败: " + e.getMessage());
        }
    }

    /**
     * 轮询扫码状态
     * 前端在展示二维码后，需要定时调用此接口（建议1-2秒轮询一次）来检查用户是否已扫码
     */
    @GetMapping("/scan-status")
    @Operation(
            summary = "轮询扫码状态",
            description = "前端定时调用此接口查询用户扫码状态。建议每1-2秒轮询一次，直到状态变为CONFIRMED或EXPIRED"
    )
    @RateLimit(key = "wechat:scan-status", rate = 2)
    public R<WeChatScanStatusResponse> pollScanStatus(
            @RequestParam
            @NotBlank(message = "state参数不能为空")
            @Parameter(description = "state参数", example = "random_state_string", required = true)
            String state) {

        log.debug("轮询微信扫码状态: state={}", state);

        try {
            WeChatScanStatusResponse response = qrCodeService.pollScanStatus(state);
            return R.success(response);
        } catch (Exception e) {
            log.error("查询扫码状态失败: state={}", state, e);
            return R.fail("查询扫码状态失败: " + e.getMessage());
        }
    }

    /**
     * 微信授权回调接口
     * 用户扫码确认后，微信会重定向到此接口，携带授权码
     */
    @GetMapping("/callback")
    @Operation(
            summary = "微信授权回调",
            description = "微信授权回调接口，用户扫码确认后微信会调用此接口。通常由微信服务器调用，也可以前端接收参数后调用"
    )
    @RateLimit(key = "wechat:callback", rate = 10)
    public R<AuthResponse> handleCallback(
            @RequestParam
            @NotBlank(message = "授权码不能为空")
            @Parameter(description = "微信授权码", example = "code_xxx", required = true)
            String code,

            @RequestParam
            @NotBlank(message = "state参数不能为空")
            @Parameter(description = "防CSRF攻击的state参数", example = "random_state_string", required = true)
            String state) {

        log.info("微信授权回调: code={}, state={}", code, state);

        try {
            // 1. 更新扫码状态为已确认（可选，用于轮询接口返回）
            qrCodeService.updateScanStatusWithCode(state, code);

            // 2. 构建登录请求
            OAuth2LoginRequest loginRequest = new OAuth2LoginRequest();
            loginRequest.setType(LoginType.OAUTH2_WECHAT);
            loginRequest.setCode(code);
            loginRequest.setState(state);
            loginRequest.setProvider("wechat");

            // 3. 执行登录流程
            AuthResponse authResponse = loginService.login(loginRequest);

            AuthUserInfoVO userInfo = authResponse.getUserInfo();

            log.info("微信登录成功: memberId={}", userInfo.getMemberId());

            return R.success(authResponse);
        } catch (Exception e) {
            log.error("微信登录失败: code={}, state={}", code, state, e);
            return R.fail("微信登录失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发登录（给前端使用）
     * 前端接收到微信回调后，可以通过此接口主动触发登录
     */
    @PostMapping("/login")
    @Operation(
            summary = "微信登录",
            description = "使用微信授权码完成登录。前端接收到code和state后调用此接口"
    )
    @RateLimit(key = "wechat:login", rate = 10)
    public R<AuthResponse> login(
            @RequestParam
            @NotBlank(message = "授权码不能为空")
            @Parameter(description = "微信授权码", example = "code_xxx", required = true)
            String code,

            @RequestParam
            @NotBlank(message = "state参数不能为空")
            @Parameter(description = "防CSRF攻击的state参数", example = "random_state_string", required = true)
            String state) {

        return handleCallback(code, state);
    }

    /**
     * 获取微信登录配置信息（可选）
     */
    @GetMapping("/config")
    @Operation(
            summary = "获取微信登录配置",
            description = "获取微信登录的配置信息，如AppID等（不包含Secret）"
    )
    public R<Object> getConfig() {
        // 返回一些公开的配置信息，方便前端使用
        return R.success("微信扫码登录已启用");
    }
}

