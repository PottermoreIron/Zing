package com.pot.auth.domain.shared.enums;

import com.pot.zing.framework.common.service.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Auth服务错误码枚举
 *
 * <p>错误码规范：
 * <ul>
 *   <li>A0001-A0099: 认证相关错误</li>
 *   <li>A0100-A0199: Token相关错误</li>
 *   <li>A0200-A0299: 验证码相关错误</li>
 *   <li>A0300-A0399: 注册相关错误</li>
 *   <li>A0400-A0499: 权限相关错误</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-10
 */
@Getter
@AllArgsConstructor
public enum AuthResultCode implements IResultCode {

    // ========== 认证相关错误 AUTH_0001-AUTH_0099 ==========

    /**
     * 认证失败 - 用户名或密码错误
     */
    AUTHENTICATION_FAILED("AUTH_0001", "用户名或密码错误", false),

    /**
     * 账户已锁定
     */
    ACCOUNT_LOCKED("AUTH_0002", "账户已被锁定，请联系管理员", false),

    /**
     * 账户已禁用
     */
    ACCOUNT_DISABLED("AUTH_0003", "账户已被禁用", false),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND("AUTH_0004", "用户不存在", false),

    /**
     * 密码错误次数过多
     */
    PASSWORD_RETRY_LIMIT_EXCEEDED("AUTH_0005", "密码错误次数过多，账户已被锁定", false),

    // ========== Token相关错误 AUTH_0100-AUTH_0199 ==========

    /**
     * Token已过期
     */
    TOKEN_EXPIRED("AUTH_0100", "Token已过期", false),

    /**
     * Token无效
     */
    TOKEN_INVALID("AUTH_0101", "Token无效", false),

    /**
     * Token已失效（在黑名单中）
     */
    TOKEN_REVOKED("AUTH_0102", "Token已失效", false),

    /**
     * Token解析失败
     */
    TOKEN_PARSE_ERROR("AUTH_0103", "Token解析失败", false),

    /**
     * RefreshToken已过期
     */
    REFRESH_TOKEN_EXPIRED("AUTH_0104", "RefreshToken已过期，请重新登录", false),

    /**
     * RefreshToken无效
     */
    REFRESH_TOKEN_INVALID("AUTH_0105", "RefreshToken无效，请重新登录", false),

    // ========== 验证码相关错误 A0200-A0299 ==========

    /**
     * 验证码发送过于频繁
     */
    CODE_SEND_TOO_FREQUENT("AUTH_0200", "验证码发送过于频繁，请稍后再试", false),

    /**
     * 验证码不存在或已过期
     */
    CODE_NOT_FOUND("AUTH_0201", "验证码不存在或已过期", false),

    /**
     * 验证码错误
     */
    CODE_MISMATCH("AUTH_0202", "验证码错误", false),

    /**
     * 验证次数超限
     */
    CODE_VERIFICATION_EXCEEDED("AUTH_0203", "验证次数超限，请重新获取验证码", false),

    /**
     * 验证码格式错误
     */
    CODE_FORMAT_INVALID("AUTH_0204", "验证码格式错误", false),

    /**
     * 验证码发送失败
     */
    CODE_SEND_FAILED("AUTH_0205", "验证码发送失败，请稍后重试", false),

    /**
     * 验证码无效（已使用或已过期）
     */
    VERIFICATION_CODE_INVALID("AUTH_0206", "验证码无效或已过期", false),

    // ========== 注册相关错误 AUTH_0300-AUTH_0399 ==========

    /**
     * 用户名已存在
     */
    USERNAME_ALREADY_EXISTS("AUTH_0300", "用户名已存在", false),

    /**
     * 邮箱已存在
     */
    EMAIL_ALREADY_EXISTS("AUTH_0301", "邮箱已被注册", false),

    /**
     * 手机号已存在
     */
    PHONE_ALREADY_EXISTS("AUTH_0302", "手机号已被注册", false),

    /**
     * 密码强度不足
     */
    WEAK_PASSWORD("AUTH_0303", "密码强度不足", false),

    /**
     * 邮箱格式错误
     */
    INVALID_EMAIL("AUTH_0304", "邮箱格式不正确", false),

    /**
     * 手机号格式错误
     */
    INVALID_PHONE("AUTH_0305", "手机号格式不正确", false),

    /**
     * 不支持的登录类型
     */
    UNSUPPORTED_LOGIN_TYPE("AUTH_0306", "不支持的登录类型", false),

    /**
     * 不支持的注册类型
     */
    UNSUPPORTED_REGISTER_TYPE("AUTH_0307", "不支持的注册类型", false),

    /**
     * 不支持的认证类型
     */
    UNSUPPORTED_AUTHENTICATION_TYPE("AUTH_0308", "不支持的认证类型", false),

    /**
     * 无效的注册请求
     */
    INVALID_REGISTER_REQUEST("AUTH_0309", "无效的注册请求", false),

    /**
     * 无效的登录请求
     */
    INVALID_LOGIN_REQUEST("AUTH_0310", "无效的登录请求", false),

    // ========== 权限相关错误 AUTH_0400-AUTH_0499 ==========

    /**
     * 无权限
     */
    PERMISSION_DENIED("AUTH_0400", "无权限访问", false),

    /**
     * 角色不存在
     */
    ROLE_NOT_FOUND("AUTH_0401", "角色不存在", false),

    // ========== 参数验证错误 AUTH_0500-AUTH_0599 ==========

    /**
     * 参数错误
     */
    INVALID_PARAMETER("AUTH_0500", "参数错误", false),

    /**
     * 用户域不支持
     */
    UNSUPPORTED_USER_DOMAIN("AUTH_0501", "不支持的用户域", false),

    // ========== 系统错误 AUTH_0900-AUTH_0999 ==========

    /**
     * 系统异常
     */
    SYSTEM_ERROR("AUTH_0900", "系统异常，请稍后重试", false);

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误信息
     */
    private final String msg;

    /**
     * 是否成功
     */
    private final boolean success;
}

