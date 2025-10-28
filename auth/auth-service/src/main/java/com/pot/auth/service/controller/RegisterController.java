package com.pot.auth.service.controller;

import com.pot.auth.service.dto.request.register.RegisterRequest;
import com.pot.auth.service.dto.response.RegisterResponse;
import com.pot.auth.service.service.RegisterService;
import com.pot.zing.framework.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 注册控制器 - 提供多种用户注册方式的统一入口
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>支持多种注册方式：用户名密码、手机号密码、邮箱密码、手机验证码、邮箱验证码</li>
 *   <li>采用策略模式，根据注册类型自动路由到对应的处理策略</li>
 *   <li>统一的请求验证和异常处理</li>
 *   <li>完整的操作日志记录</li>
 *   <li>RESTful API 设计规范</li>
 * </ul>
 *
 * <p>扩展性设计：</p>
 * <ul>
 *   <li>新增注册方式只需添加对应的 Request 类型和 Strategy 实现</li>
 *   <li>支持自定义注册后的业务逻辑扩展</li>
 *   <li>可配置的注册流程控制</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户注册管理", description = "用户注册相关接口，支持多种注册方式")
public class RegisterController {

    private final RegisterService registerService;

    /**
     * 用户注册统一入口
     *
     * <p>支持的注册类型：</p>
     * <ul>
     *   <li>USERNAME_PASSWORD(1): 用户名 + 密码注册</li>
     *   <li>PHONE_PASSWORD(2): 手机号 + 密码注册</li>
     *   <li>EMAIL_PASSWORD(3): 邮箱 + 密码注册</li>
     *   <li>PHONE_CODE(4): 手机号 + 验证码注册</li>
     *   <li>EMAIL_CODE(5): 邮箱 + 验证码注册</li>
     * </ul>
     *
     * <p>注册成功后，系统将：</p>
     * <ul>
     *   <li>创建用户账户</li>
     *   <li>生成认证令牌（AccessToken 和 RefreshToken）</li>
     *   <li>返回用户基本信息</li>
     * </ul>
     *
     * @param request 注册请求对象，根据 type 字段自动识别并反序列化为对应的具体类型
     * @return 注册响应，包含认证令牌和用户信息
     */
    @PostMapping("/register")
    @Operation(
            summary = "用户注册",
            description = "支持多种注册方式的统一注册接口。根据 type 字段自动识别注册类型：" +
                    "1-用户名密码、2-手机号密码、3-邮箱密码、4-手机验证码、5-邮箱验证码。" +
                    "注册成功后自动完成登录，返回访问令牌和用户信息。"
    )
    public R<RegisterResponse> register(
            @Valid @RequestBody
            @Parameter(description = "注册请求，根据 type 字段自动识别注册方式并验证对应的必填字段")
            RegisterRequest request) {
        log.info("收到用户注册请求: type={}", request.getType());

        try {
            RegisterResponse response = registerService.register(request);
            log.info("用户注册成功: type={}, userId={}",
                    request.getType(),
                    response.getAuthResponse() != null && response.getAuthResponse().getUserInfo() != null
                            ? response.getAuthResponse().getUserInfo().getMemberId()
                            : "unknown");
            return R.success(response);
        } catch (Exception e) {
            log.error("用户注册失败: type={}, error={}", request.getType(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 检查用户名是否可用
     *
     * <p>用于注册前的用户名唯一性校验，避免用户提交后才发现用户名已被占用</p>
     *
     * @param username 待检查的用户名
     * @return true 表示用户名可用，false 表示已被占用
     */
    @GetMapping("/register/check/username")
    @Operation(
            summary = "检查用户名是否可用",
            description = "用于注册前验证用户名是否已被占用，提升用户体验"
    )
    public R<Boolean> checkUsernameAvailability(
            @RequestParam
            @Parameter(description = "待检查的用户名")
            String username) {
        log.info("检查用户名可用性: username={}", username);
        // TODO: 实现用户名可用性检查逻辑
        // return R.success(registerService.checkUsernameAvailability(username));
        return R.success(true);
    }

    /**
     * 检查手机号是否已注册
     *
     * <p>用于注册前的手机号唯一性校验</p>
     *
     * @param phone 待检查的手机号
     * @return true 表示手机号可用（未注册），false 表示已注册
     */
    @GetMapping("/register/check/phone")
    @Operation(
            summary = "检查手机号是否已注册",
            description = "用于注册前验证手机号是否已被注册，避免重复注册"
    )
    public R<Boolean> checkPhoneAvailability(
            @RequestParam
            @Parameter(description = "待检查的手机号")
            String phone) {
        log.info("检查手机号可用性: phone={}", phone);
        // TODO: 实现手机号可用性检查逻辑
        // return R.success(registerService.checkPhoneAvailability(phone));
        return R.success(true);
    }

    /**
     * 检查邮箱是否已注册
     *
     * <p>用于注册前的邮箱唯一性校验</p>
     *
     * @param email 待检查的邮箱地址
     * @return true 表示邮箱可用（未注册），false 表示已注册
     */
    @GetMapping("/register/check/email")
    @Operation(
            summary = "检查邮箱是否已注册",
            description = "用于注册前验证邮箱是否已被注册，避免重复注册"
    )
    public R<Boolean> checkEmailAvailability(
            @RequestParam
            @Parameter(description = "待检查的邮箱地址")
            String email) {
        log.info("检查邮箱可用性: email={}", email);
        // TODO: 实现邮箱可达性检查逻辑
        // return R.success(registerService.checkEmailAvailability(email));
        return R.success(true);
    }

    /**
     * 发送注册验证码
     *
     * <p>用于验证码注册流程，向用户手机或邮箱发送验证码</p>
     *
     * @param type   验证码类型：phone-手机验证码、email-邮箱验证码
     * @param target 目标地址：手机号或邮箱地址
     * @return 发送结果
     */
    @PostMapping("/register/verification-code/send")
    @Operation(
            summary = "发送注册验证码",
            description = "向手机号或邮箱发送注册验证码，用于验证码注册流程"
    )
    public R<Void> sendVerificationCode(
            @RequestParam
            @Parameter(description = "验证码类型：phone-手机验证码、email-邮箱验证码")
            String type,
            @RequestParam
            @Parameter(description = "目标地址：手机号或邮箱地址")
            String target) {
        log.info("发送注册验证码: type={}, target={}", type, target);
        // TODO: 实现验证码发送逻辑
        // registerService.sendVerificationCode(type, target);
        return R.success();
    }
}

