package com.pot.auth.service.controller.v1;

import com.pot.auth.service.dto.v1.request.ChangePasswordRequest;
import com.pot.auth.service.dto.v1.request.ResetPasswordRequest;
import com.pot.auth.service.dto.v1.request.SendVerificationCodeRequest;
import com.pot.auth.service.dto.v1.request.VerifyCodeRequest;
import com.pot.auth.service.service.v1.CredentialService;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.touch.model.VerificationCodeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 凭证管理控制器
 * <p>
 * 职责：
 * 1. 验证码发送与验证
 * 2. 密码修改与重置
 * 3. 凭证有效性检查
 * <p>
 * URL设计：
 * - POST /api/v1/auth/credentials/verification-codes         发送验证码
 * - POST /api/v1/auth/credentials/verification-codes/verify  验证验证码
 * - PUT  /api/v1/auth/credentials/password                   修改密码
 * - POST /api/v1/auth/credentials/password/reset             重置密码
 *
 * @author Pot
 * @since 2025-10-25
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/credentials")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "2. 凭证管理",
        description = "Credential Management - 验证码、密码等凭证的管理"
)
public class CredentialController {

    private final CredentialService credentialService;

    /**
     * 发送验证码
     */
    @PostMapping("/verification-codes")
    @Operation(
            summary = "发送验证码",
            description = """
                    发送短信或邮箱验证码
                    
                    **支持的类型**：
                    - sms: 短信验证码
                    - email: 邮箱验证码
                    
                    **支持的用途**：
                    - login: 登录
                    - register: 注册
                    - reset_password: 重置密码
                    - bind_phone: 绑定手机号
                    - bind_email: 绑定邮箱
                    
                    **安全限制**：
                    - 同一手机号/邮箱：1次/分钟，10次/小时
                    - 验证码有效期：5分钟
                    - 验证码长度：6位数字
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "发送短信验证码",
                                            value = """
                                                    {
                                                      "type": "sms",
                                                      "recipient": "13800138000",
                                                      "purpose": "login"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "发送邮箱验证码",
                                            value = """
                                                    {
                                                      "type": "email",
                                                      "recipient": "user@example.com",
                                                      "purpose": "register"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    public R<VerificationCodeResponse> sendVerificationCode(
            @Valid @RequestBody SendVerificationCodeRequest request
    ) {
        log.info("发送验证码请求: type={}, recipient={}, purpose={}",
                request.getType(), request.getRecipient(), request.getPurpose());

        VerificationCodeResponse response = credentialService.sendVerificationCode(
                request.getType(), request.getRecipient(), request.getPurpose());

        log.info("验证码发送成功: type={}, recipient={}", request.getType(), request.getRecipient());
        return R.success(response, "验证码已发送");
    }

    /**
     * 验证验证码
     */
    @PostMapping("/verification-codes/verify")
    @Operation(
            summary = "验证验证码",
            description = """
                    验证短信或邮箱验证码是否正确
                    
                    **注意**：
                    - 此接口仅验证验证码，不执行登录或注册
                    - 验证成功后验证码仍然有效，可用于后续操作
                    - 用于前端提前验证，提升用户体验
                    """
    )
    public R<Boolean> verifyCode(
            @Valid @RequestBody VerifyCodeRequest request
    ) {
        log.info("验证验证码请求: type={}, recipient={}", request.getType(), request.getRecipient());

        boolean valid = credentialService.verifyCode(request.getType(), request.getRecipient(), request.getCode());

        return R.success(valid, valid ? "验证码正确" : "验证码错误或已过期");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(
            summary = "修改密码",
            description = """
                    修改当前用户的密码
                    
                    **要求**：
                    - 必须提供正确的旧密码
                    - 新密码必须符合密码策略
                    - 修改成功后，所有会话将失效（需重新登录）
                    
                    **密码策略**：
                    - 长度：8-20位
                    - 必须包含：大写字母、小写字母、数字
                    - 可选包含：特殊字符
                    - 不能与旧密码相同
                    - 不能包含用户名
                    """
    )
    public R<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        log.info("修改密码请求: userId={}", com.pot.zing.framework.common.util.SecurityUtils.getCurrentUserId());

        credentialService.changePassword(
                request.getOldPassword(), request.getNewPassword());

        return R.success(null, "密码修改成功，请重新登录");
    }

    /**
     * 重置密码
     */
    @PostMapping("/password/reset")
    @Operation(
            summary = "重置密码",
            description = """
                    通过验证码重置密码（忘记密码场景）
                    
                    **流程**：
                    1. 用户先调用发送验证码接口
                    2. 用户输入验证码和新密码
                    3. 系统验证验证码
                    4. 重置密码成功
                    
                    **注意**：
                    - 验证码验证成功后立即失效
                    - 重置成功后，所有会话将失效
                    - credential可以是手机号或邮箱
                    """
    )
    public R<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        log.info("重置密码请求: credential={}", request.getCredential());

        credentialService.resetPassword(
                request.getCredential(), request.getCode(), request.getNewPassword());

        return R.success(null, "密码重置成功");
    }
}

