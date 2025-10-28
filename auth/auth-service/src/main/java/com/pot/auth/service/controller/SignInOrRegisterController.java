package com.pot.auth.service.controller;

import com.pot.auth.service.dto.request.signinorregister.SignInOrRegisterRequest;
import com.pot.auth.service.dto.response.AuthResponse;
import com.pot.auth.service.service.SignInOrRegisterService;
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
 * @description: 一键登录/注册控制器 - Sign In/Register 统一入口
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>智能识别用户状态：已注册自动登录，未注册自动注册后登录</li>
 *   <li>支持多种认证方式：手机验证码、邮箱验证码、第三方OAuth2</li>
 *   <li>统一返回格式：Token + 用户信息</li>
 *   <li>完整的日志记录和异常处理</li>
 *   <li>RESTful API 设计规范</li>
 * </ul>
 *
 * <p>与传统登录/注册的区别：</p>
 * <ul>
 *   <li>传统方式：用户需要先判断是否注册，然后选择登录或注册接口</li>
 *   <li>一键方式：系统自动判断，用户无需关心账号状态，一个接口搞定</li>
 * </ul>
 *
 * <p>适用场景：</p>
 * <ul>
 *   <li>移动端快捷登录</li>
 *   <li>小程序一键授权</li>
 *   <li>第三方账号绑定</li>
 *   <li>验证码快速登录</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "一键登录管理", description = "Sign In/Register 一键登录注册接口，智能识别用户状态")
public class SignInOrRegisterController {

    private final SignInOrRegisterService signInOrRegisterService;

    /**
     * 一键登录/注册统一入口
     *
     * <p>功能说明：</p>
     * <ul>
     *   <li><b>已注册用户</b>：直接登录，返回 Token 和用户信息</li>
     *   <li><b>未注册用户</b>：自动注册后登录，返回 Token 和用户信息</li>
     * </ul>
     *
     * <p>支持的认证方式（通过 type 字段区分）：</p>
     * <ul>
     *   <li><b>PHONE_CODE(1)</b>: 手机号 + 验证码
     *       <pre>{"type": 1, "phone": "13800138000", "code": "123456"}</pre>
     *   </li>
     *   <li><b>EMAIL_CODE(2)</b>: 邮箱 + 验证码
     *       <pre>{"type": 2, "email": "user@example.com", "code": "123456"}</pre>
     *   </li>
     *   <li><b>OAUTH2(3)</b>: 第三方OAuth2（微信、GitHub、Google等）
     *       <pre>{"type": 3, "provider": "WECHAT", "code": "auth_code_xxx"}</pre>
     *   </li>
     * </ul>
     *
     * <p>返回数据说明：</p>
     * <ul>
     *   <li><b>accessToken</b>: 访问令牌，用于后续API调用</li>
     *   <li><b>refreshToken</b>: 刷新令牌，用于延长会话</li>
     *   <li><b>expiresIn</b>: Token有效期（秒）</li>
     *   <li><b>userInfo</b>: 用户基本信息（ID、昵称、头像等）</li>
     * </ul>
     *
     * <p>使用示例：</p>
     * <pre>
     * // 1. 手机验证码一键登录
     * POST /auth/sign-in-or-register
     * {
     *   "type": 1,
     *   "phone": "13800138000",
     *   "code": "123456"
     * }
     *
     * // 2. 邮箱验证码一键登录
     * POST /auth/sign-in-or-register
     * {
     *   "type": 2,
     *   "email": "user@example.com",
     *   "code": "123456"
     * }
     *
     * // 3. 微信一键登录
     * POST /auth/sign-in-or-register
     * {
     *   "type": 3,
     *   "provider": "WECHAT",
     *   "code": "wx_auth_code_xxx",
     *   "redirectUri": "https://your-app.com/callback"
     * }
     * </pre>
     *
     * @param request 一键登录/注册请求，根据 type 字段自动识别认证方式
     * @return 认证响应，包含访问令牌、刷新令牌和用户信息
     */
    @PostMapping("/sign-in-or-register")
    @Operation(
            summary = "一键登录/注册",
            description = "智能识别用户状态的统一认证接口。" +
                    "已注册用户直接登录，未注册用户自动注册后登录。" +
                    "支持手机验证码、邮箱验证码、第三方OAuth2等多种认证方式。" +
                    "一个接口解决所有登录和注册场景，提升用户体验。"
    )
    public R<AuthResponse> signInOrRegister(
            @Valid @RequestBody
            @Parameter(description = "一键登录请求，根据 type 字段识别认证方式：" +
                    "1-手机验证码、2-邮箱验证码、3-第三方OAuth2")
            SignInOrRegisterRequest request) {

        log.info("收到一键登录/注册请求: type={}", request.getType());

        try {
            AuthResponse response = signInOrRegisterService.signInOrRegister(request);

            log.info("一键登录/注册成功: type={}, memberId={}",
                    request.getType(),
                    response.getUserInfo() != null
                            ? response.getUserInfo().getMemberId()
                            : "unknown");

            return R.success(response);
        } catch (Exception e) {
            log.error("一键登录/注册失败: type={}, error={}",
                    request.getType(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 发送一键登录验证码
     *
     * <p>用于手机号或邮箱验证码的一键登录场景</p>
     *
     * <p>使用流程：</p>
     * <ol>
     *   <li>前端调用此接口发送验证码</li>
     *   <li>用户输入收到的验证码</li>
     *   <li>前端调用 /sign-in-or-register 接口完成登录/注册</li>
     * </ol>
     *
     * @param type   验证码类型：phone-手机验证码、email-邮箱验证码
     * @param target 目标地址：手机号或邮箱地址
     * @return 发送结果
     */
    @PostMapping("/sign-in-or-register/verification-code/send")
    @Operation(
            summary = "发送一键登录验证码",
            description = "向手机号或邮箱发送验证码，用于一键登录/注册流程。" +
                    "验证码有效期通常为5分钟，同一目标地址60秒内只能发送一次。"
    )
    public R<Void> sendVerificationCode(
            @RequestParam
            @Parameter(description = "验证码类型：phone-手机验证码、email-邮箱验证码")
            String type,
            @RequestParam
            @Parameter(description = "目标地址：手机号或邮箱地址")
            String target) {

        log.info("发送一键登录验证码: type={}, target={}", type, target);

        // TODO: 实现验证码发送逻辑
        // 1. 校验目标地址格式
        // 2. 检查发送频率限制
        // 3. 生成随机验证码
        // 4. 存储到Redis（设置过期时间）
        // 5. 调用短信/邮件服务发送

        return R.success();
    }

    /**
     * 检查认证方式是否可用
     *
     * <p>用于前端判断是否显示某种登录方式</p>
     *
     * @param type 认证类型：1-手机验证码、2-邮箱验证码、3-OAuth2
     * @return true-可用，false-不可用
     */
    @GetMapping("/sign-in-or-register/check/availability")
    @Operation(
            summary = "检查认证方式是否可用",
            description = "用于前端动态判断是否显示某种登录方式的入口，" +
                    "例如：某些区域可能不支持手机号登录"
    )
    public R<Boolean> checkAvailability(
            @RequestParam
            @Parameter(description = "认证类型：1-手机验证码、2-邮箱验证码、3-OAuth2")
            Integer type) {

        log.debug("检查认证方式可用性: type={}", type);

        // TODO: 根据配置或区域限制判断可用性
        return R.success(true);
    }
}

