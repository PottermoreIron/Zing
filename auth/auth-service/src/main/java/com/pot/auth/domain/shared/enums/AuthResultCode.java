package com.pot.auth.domain.shared.enums;

import com.pot.zing.framework.common.service.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthResultCode implements IResultCode {


        AUTHENTICATION_FAILED("AUTH_0001", "Invalid username or password", false),

        ACCOUNT_LOCKED("AUTH_0002", "Account is locked, please contact your administrator", false),

        ACCOUNT_DISABLED("AUTH_0003", "Account is disabled", false),

        USER_NOT_FOUND("AUTH_0004", "User not found", false),

        PASSWORD_RETRY_LIMIT_EXCEEDED("AUTH_0005", "Too many failed attempts, account has been locked", false),


        TOKEN_EXPIRED("AUTH_0100", "Token has expired", false),

        TOKEN_INVALID("AUTH_0101", "Token is invalid", false),

        TOKEN_REVOKED("AUTH_0102", "Token has been revoked", false),

        TOKEN_PARSE_ERROR("AUTH_0103", "Failed to parse token", false),

        REFRESH_TOKEN_EXPIRED("AUTH_0104", "Refresh token has expired, please sign in again", false),

        REFRESH_TOKEN_INVALID("AUTH_0105", "Refresh token is invalid, please sign in again", false),


        CODE_SEND_TOO_FREQUENT("AUTH_0200", "Verification code sent too frequently, please try again later", false),

        CODE_NOT_FOUND("AUTH_0201", "Verification code not found or expired", false),

        CODE_MISMATCH("AUTH_0202", "Incorrect verification code", false),

        CODE_VERIFICATION_EXCEEDED("AUTH_0203", "Verification attempt limit exceeded, please request a new code", false),

        CODE_FORMAT_INVALID("AUTH_0204", "Invalid verification code format", false),

        CODE_SEND_FAILED("AUTH_0205", "Failed to send verification code, please try again later", false),

        VERIFICATION_CODE_INVALID("AUTH_0206", "Verification code is invalid or expired", false),


        USERNAME_ALREADY_EXISTS("AUTH_0300", "Username already taken", false),

        EMAIL_ALREADY_EXISTS("AUTH_0301", "Email is already registered", false),

        PHONE_ALREADY_EXISTS("AUTH_0302", "Phone number is already registered", false),

        WEAK_PASSWORD("AUTH_0303", "Password is too weak", false),

        INVALID_EMAIL("AUTH_0304", "Invalid email format", false),

        INVALID_PHONE("AUTH_0305", "Invalid phone number format", false),

        UNSUPPORTED_LOGIN_TYPE("AUTH_0306", "Unsupported login type", false),

        UNSUPPORTED_REGISTER_TYPE("AUTH_0307", "Unsupported registration type", false),

        UNSUPPORTED_AUTHENTICATION_TYPE("AUTH_0308", "Unsupported authentication type", false),

        INVALID_REGISTER_REQUEST("AUTH_0309", "Invalid registration request", false),

        INVALID_LOGIN_REQUEST("AUTH_0310", "Invalid login request", false),


        PERMISSION_DENIED("AUTH_0400", "Access denied", false),

        ROLE_NOT_FOUND("AUTH_0401", "Role not found", false),


        INVALID_PARAMETER("AUTH_0500", "Invalid parameter", false),

        UNSUPPORTED_USER_DOMAIN("AUTH_0501", "Unsupported user domain", false),


        SYSTEM_ERROR("AUTH_0900", "System error, please try again later", false);

        private final String code;

        private final String msg;

        private final boolean success;
}

