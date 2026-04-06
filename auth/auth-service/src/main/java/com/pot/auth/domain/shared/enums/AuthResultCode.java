package com.pot.auth.domain.shared.enums;

import com.pot.zing.framework.common.service.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthResultCode implements IResultCode {


        AUTHENTICATION_FAILED("AUTH_0001", "用户名或密码错误", false),

        ACCOUNT_LOCKED("AUTH_0002", "账户已被锁定，请联系管理员", false),

        ACCOUNT_DISABLED("AUTH_0003", "账户已被禁用", false),

        USER_NOT_FOUND("AUTH_0004", "用户不存在", false),

        PASSWORD_RETRY_LIMIT_EXCEEDED("AUTH_0005", "密码错误次数过多，账户已被锁定", false),


        TOKEN_EXPIRED("AUTH_0100", "Token已过期", false),

        TOKEN_INVALID("AUTH_0101", "Token无效", false),

        TOKEN_REVOKED("AUTH_0102", "Token已失效", false),

        TOKEN_PARSE_ERROR("AUTH_0103", "Token解析失败", false),

        REFRESH_TOKEN_EXPIRED("AUTH_0104", "RefreshToken已过期，请重新登录", false),

        REFRESH_TOKEN_INVALID("AUTH_0105", "RefreshToken无效，请重新登录", false),


        CODE_SEND_TOO_FREQUENT("AUTH_0200", "验证码发送过于频繁，请稍后再试", false),

        CODE_NOT_FOUND("AUTH_0201", "验证码不存在或已过期", false),

        CODE_MISMATCH("AUTH_0202", "验证码错误", false),

        CODE_VERIFICATION_EXCEEDED("AUTH_0203", "验证次数超限，请重新获取验证码", false),

        CODE_FORMAT_INVALID("AUTH_0204", "验证码格式错误", false),

        CODE_SEND_FAILED("AUTH_0205", "验证码发送失败，请稍后重试", false),

        VERIFICATION_CODE_INVALID("AUTH_0206", "验证码无效或已过期", false),


        USERNAME_ALREADY_EXISTS("AUTH_0300", "用户名已存在", false),

        EMAIL_ALREADY_EXISTS("AUTH_0301", "邮箱已被注册", false),

        PHONE_ALREADY_EXISTS("AUTH_0302", "手机号已被注册", false),

        WEAK_PASSWORD("AUTH_0303", "密码强度不足", false),

        INVALID_EMAIL("AUTH_0304", "邮箱格式不正确", false),

        INVALID_PHONE("AUTH_0305", "手机号格式不正确", false),

        UNSUPPORTED_LOGIN_TYPE("AUTH_0306", "不支持的登录类型", false),

        UNSUPPORTED_REGISTER_TYPE("AUTH_0307", "不支持的注册类型", false),

        UNSUPPORTED_AUTHENTICATION_TYPE("AUTH_0308", "不支持的认证类型", false),

        INVALID_REGISTER_REQUEST("AUTH_0309", "无效的注册请求", false),

        INVALID_LOGIN_REQUEST("AUTH_0310", "无效的登录请求", false),


        PERMISSION_DENIED("AUTH_0400", "无权限访问", false),

        ROLE_NOT_FOUND("AUTH_0401", "角色不存在", false),


        INVALID_PARAMETER("AUTH_0500", "参数错误", false),

        UNSUPPORTED_USER_DOMAIN("AUTH_0501", "不支持的用户域", false),


        SYSTEM_ERROR("AUTH_0900", "系统异常，请稍后重试", false);

        private final String code;

        private final String msg;

        private final boolean success;
}

