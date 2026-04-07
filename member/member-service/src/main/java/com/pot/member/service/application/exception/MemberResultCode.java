package com.pot.member.service.application.exception;

import com.pot.zing.framework.common.service.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Result codes for member-service business failures.
 */
@Getter
@AllArgsConstructor
public enum MemberResultCode implements IResultCode {
    MEMBER_NOT_FOUND("MEMBER_0001", "会员不存在", false),
    NICKNAME_ALREADY_EXISTS("MEMBER_0002", "昵称已被使用", false),
    EMAIL_ALREADY_EXISTS("MEMBER_0003", "邮箱已被注册", false),
    PHONE_ALREADY_EXISTS("MEMBER_0004", "手机号已被注册", false),
    PASSWORD_INCORRECT("MEMBER_0005", "密码不正确", false),
    ACCOUNT_UNAVAILABLE("MEMBER_0006", "账户已被禁用或锁定", false),
    REGISTRATION_CONFLICT("MEMBER_0007", "注册信息冲突，请检查后重试", false);

    private final String code;
    private final String msg;
    private final boolean success;
}