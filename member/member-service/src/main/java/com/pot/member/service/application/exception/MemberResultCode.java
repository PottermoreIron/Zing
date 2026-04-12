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
    MEMBER_NOT_FOUND("MEMBER_0001", "Member not found", false),
    NICKNAME_ALREADY_EXISTS("MEMBER_0002", "Nickname already in use", false),
    EMAIL_ALREADY_EXISTS("MEMBER_0003", "Email already registered", false),
    PHONE_ALREADY_EXISTS("MEMBER_0004", "Phone number already registered", false),
    PASSWORD_INCORRECT("MEMBER_0005", "Incorrect password", false),
    ACCOUNT_UNAVAILABLE("MEMBER_0006", "Account is disabled or locked", false),
    REGISTRATION_CONFLICT("MEMBER_0007", "Registration conflict, please check your details and try again", false);

    private final String code;
    private final String msg;
    private final boolean success;
}