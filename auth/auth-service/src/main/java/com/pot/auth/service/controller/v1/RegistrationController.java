package com.pot.auth.service.controller.v1;

import com.pot.auth.service.dto.request.AvailabilityCheckRequest;
import com.pot.auth.service.dto.request.RegistrationRequest;
import com.pot.auth.service.dto.response.AvailabilityCheckResponse;
import com.pot.auth.service.dto.v1.session.AuthSession;
import com.pot.auth.service.service.v1.RegistrationService;
import com.pot.zing.framework.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 用户注册控制器
 * <p>
 * 提供用户注册、可用性检查等功能
 * 符合RESTful规范，注册作为创建用户资源的操作
 *
 * @author Zing
 * @since 2025-10-25
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/registrations")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户注册", description = "用户注册和可用性检查接口")
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * 用户注册
     * <p>
     * 创建新用户账户，支持多种注册方式：
     * - 用户名密码注册
     * - 手机号验证码注册
     * - 邮箱验证码注册
     * <p>
     * 注册成功后自动登录，返回认证会话
     *
     * @param request 注册请求
     * @return 认证会话（自动登录）
     */
    @PostMapping
    @Operation(
            summary = "用户注册",
            description = "创建新用户账户并自动登录。支持用户名密码、手机号验证码、邮箱验证码等多种注册方式"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "注册成功",
                    content = @Content(schema = @Schema(implementation = AuthSession.class))
            ),
            @ApiResponse(responseCode = "400", description = "请求参数错误或验证码无效"),
            @ApiResponse(responseCode = "409", description = "用户名/手机号/邮箱已被注册")
    })
    public R<AuthSession> register(
            @Valid @RequestBody RegistrationRequest request) {

        log.info("[RegistrationController] 用户注册, registrationType={}, identifier={}",
                request.getRegistrationType(),
                maskIdentifier(request.getIdentifier()));

        AuthSession session = registrationService.register(request);

        log.info("[RegistrationController] 注册成功, userId={}, sessionId={}",
                session.getUserInfo().getUserId(),
                session.getSessionId());

        return R.success(session, "注册成功");
    }

    /**
     * 检查用户名/手机号/邮箱可用性
     * <p>
     * 在用户输入时实时检查，提供友好的用户体验
     *
     * @param type  类型: username, phone, email
     * @param value 要检查的值
     * @return 可用性结果
     */
    @GetMapping("/availability")
    @Operation(
            summary = "检查可用性",
            description = "检查用户名、手机号或邮箱是否已被注册，用于注册表单的实时验证"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AvailabilityCheckResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    public R<AvailabilityCheckResponse> checkAvailability(
            @Parameter(description = "类型: username/phone/email", required = true)
            @RequestParam @NotBlank String type,

            @Parameter(description = "要检查的值", required = true)
            @RequestParam @NotBlank String value) {

        log.debug("[RegistrationController] 检查可用性, type={}, value={}",
                type, maskIdentifier(value));

        AvailabilityCheckRequest request = AvailabilityCheckRequest.builder()
                .type(type)
                .value(value)
                .build();

        AvailabilityCheckResponse response = registrationService.checkAvailability(request);

        return R.success(response, "查询成功");
    }

    /**
     * 批量检查可用性
     * <p>
     * 一次性检查多个字段的可用性
     *
     * @param request 批量检查请求
     * @return 可用性结果
     */
    @PostMapping("/availability/batch")
    @Operation(
            summary = "批量检查可用性",
            description = "一次性检查多个字段（用户名、手机号、邮箱）的可用性"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    public R<AvailabilityCheckResponse> batchCheckAvailability(
            @Valid @RequestBody AvailabilityCheckRequest request) {

        log.debug("[RegistrationController] 批量检查可用性");

        AvailabilityCheckResponse response = registrationService.checkAvailability(request);

        return R.success(response, "查询成功");
    }

    /**
     * 发送注册验证码
     * <p>
     * 为手机号或邮箱发送注册验证码
     * 注意：此接口为便捷接口，也可以使用CredentialController的通用验证码接口
     *
     * @param type      类型: sms, email
     * @param recipient 接收者（手机号或邮箱）
     * @return 操作结果
     */
    @PostMapping("/verification-code")
    @Operation(
            summary = "发送注册验证码",
            description = "为手机号或邮箱发送注册验证码（便捷接口）"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发送成功"),
            @ApiResponse(responseCode = "400", description = "参数错误或发送频率过高"),
            @ApiResponse(responseCode = "409", description = "该手机号/邮箱已被注册")
    })
    public R<Void> sendVerificationCode(
            @Parameter(description = "类型: sms/email", required = true)
            @RequestParam @NotBlank String type,

            @Parameter(description = "接收者（手机号或邮箱）", required = true)
            @RequestParam @NotBlank String recipient) {

        log.info("[RegistrationController] 发送注册验证码, type={}, recipient={}",
                type, maskIdentifier(recipient));

        registrationService.sendVerificationCode(type, recipient);

        log.info("[RegistrationController] 验证码发送成功");
        return R.success(null, "验证码已发送");
    }

    /**
     * 获取注册配置信息
     * <p>
     * 返回注册相关的配置，如：
     * - 是否开放注册
     * - 用户名规则
     * - 密码强度要求
     * - 支持的注册方式
     *
     * @return 注册配置
     */
    @GetMapping("/config")
    @Operation(
            summary = "获取注册配置",
            description = "获取注册相关配置信息，包括密码规则、用户名规则、是否开放注册等"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public R<Object> getRegistrationConfig() {
        log.debug("[RegistrationController] 获取注册配置");

        Object config = registrationService.getRegistrationConfig();

        return R.success(config, "查询成功");
    }

    /**
     * 脱敏标识符（日志输出用）
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 3) {
            return "***";
        }

        // 邮箱
        if (identifier.contains("@")) {
            String[] parts = identifier.split("@");
            if (parts[0].length() <= 2) {
                return "***@" + parts[1];
            }
            return parts[0].substring(0, 2) + "***@" + parts[1];
        }

        // 手机号
        if (identifier.length() == 11 && identifier.matches("^1\\d{10}$")) {
            return identifier.substring(0, 3) + "****" + identifier.substring(7);
        }

        // 其他（用户名等）
        if (identifier.length() <= 4) {
            return identifier.charAt(0) + "***";
        }
        return identifier.substring(0, 2) + "***" + identifier.substring(identifier.length() - 2);
    }
}

