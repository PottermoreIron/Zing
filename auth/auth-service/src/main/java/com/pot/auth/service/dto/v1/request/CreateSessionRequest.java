package com.pot.auth.service.dto.v1.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pot.auth.service.dto.v1.session.AuthSession;
import com.pot.auth.service.service.v1.AuthenticationService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建会话请求 - 统一登录入口
 * <p>
 * 核心设计理念：
 * 1. 所有认证方式统一为"创建会话"操作
 * 2. 通过grantType区分不同的认证方式（符合OAuth2.0标准）
 * 3. 使用Jackson多态反序列化，根据grantType自动映射到具体的Request类
 * 4. 每个具体的Request类负责自己的认证逻辑（策略模式）
 * <p>
 * 支持的grantType：
 * - password: 用户名/邮箱/手机号 + 密码登录
 * - sms_code: 短信验证码登录
 * - email_code: 邮箱验证码登录
 * - authorization_code: OAuth2授权码登录（GitHub、Google、Facebook等）
 * - wechat_qrcode: 微信扫码登录
 * - refresh_token: 刷新令牌
 * <p>
 * 扩展性设计：
 * 新增认证方式只需：
 * 1. 创建新的XxxGrantRequest类继承此类
 * 2. 在@JsonSubTypes中添加映射
 * 3. 实现authenticate方法
 * 无需修改Controller和现有代码
 *
 * @author Pot
 * @since 2025-10-25
 */
@Data
@Schema(description = "创建认证会话请求（统一登录入口）")
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "grantType",
        visible = true,
        defaultImpl = PasswordGrantRequest.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PasswordGrantRequest.class, name = "password"),
        @JsonSubTypes.Type(value = SmsCodeGrantRequest.class, name = "sms_code"),
        @JsonSubTypes.Type(value = EmailCodeGrantRequest.class, name = "email_code"),
        @JsonSubTypes.Type(value = AuthorizationCodeGrantRequest.class, name = "authorization_code"),
        @JsonSubTypes.Type(value = WeChatQrCodeGrantRequest.class, name = "wechat_qrcode"),
        @JsonSubTypes.Type(value = RefreshTokenGrantRequest.class, name = "refresh_token")
})
public abstract class CreateSessionRequest {

    /**
     * 授权类型（Grant Type）
     * <p>
     * 符合OAuth 2.0 RFC 6749标准：
     * - password: Resource Owner Password Credentials Grant
     * - authorization_code: Authorization Code Grant
     * - refresh_token: Refresh Token Grant
     * <p>
     * 扩展类型：
     * - sms_code: 短信验证码授权（自定义扩展）
     * - email_code: 邮箱验证码授权（自定义扩展）
     * - wechat_qrcode: 微信扫码授权（自定义扩展）
     */
    @NotBlank(message = "grantType不能为空")
    @Schema(
            description = "授权类型",
            example = "password",
            allowableValues = {"password", "sms_code", "email_code", "authorization_code", "wechat_qrcode", "refresh_token"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String grantType;

    /**
     * 客户端标识
     * 用于区分不同的客户端应用（Web、Android、iOS等）
     */
    @Schema(
            description = "客户端标识",
            example = "web",
            allowableValues = {"web", "android", "ios", "mini_program"}
    )
    private String clientId;

    /**
     * 设备信息
     * 用于安全审计、多设备管理、异常检测等
     */
    @Schema(description = "设备信息")
    private DeviceInfo deviceInfo;

    /**
     * 执行认证逻辑（模板方法模式）
     * <p>
     * 每个具体的Grant类型实现自己的认证逻辑
     * Controller只需调用此方法，无需关心具体实现
     *
     * @param authService 认证服务
     * @return 认证会话
     */
    public abstract AuthSession authenticate(AuthenticationService authService);

    /**
     * 设备信息
     */
    @Data
    @Schema(description = "设备信息")
    public static class DeviceInfo {

        @Schema(description = "设备类型", example = "mobile")
        private String deviceType;  // mobile, tablet, desktop, smart_tv

        @Schema(description = "操作系统", example = "iOS 16.0")
        private String platform;

        @Schema(description = "浏览器信息", example = "Chrome 119.0")
        private String browser;

        @Schema(description = "应用版本", example = "1.0.0")
        private String appVersion;

        @Schema(description = "设备唯一标识", example = "device_uuid_xxx")
        private String deviceId;

        @Schema(description = "设备名称", example = "iPhone 15 Pro")
        private String deviceName;

        @Schema(description = "推送令牌（用于消息推送）")
        private String pushToken;

        @Schema(description = "IP地址", example = "192.168.1.1")
        private String ipAddress;

        @Schema(description = "地理位置", example = "Beijing, China")
        private String location;
    }
}

