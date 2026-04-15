package com.pot.auth.domain.shared.enums;

import com.pot.zing.framework.common.service.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthResultCode implements IResultCode {

        AUTHENTICATION_FAILED("AUTH_0001", "Invalid username or password", false, 401),

        ACCOUNT_LOCKED("AUTH_0002", "Account is locked, please contact your administrator", false, 401),

        ACCOUNT_DISABLED("AUTH_0003", "Account is disabled", false, 401),

        USER_NOT_FOUND("AUTH_0004", "User not found", false, 401),

        PASSWORD_RETRY_LIMIT_EXCEEDED("AUTH_0005", "Too many failed attempts, account has been locked", false, 429),

        ACCOUNT_STATUS_ABNORMAL("AUTH_0006", "Account status is abnormal, please contact support", false, 401),

        TOKEN_EXPIRED("AUTH_0100", "Token has expired", false, 401),

        TOKEN_INVALID("AUTH_0101", "Token is invalid", false, 401),

        TOKEN_REVOKED("AUTH_0102", "Token has been revoked", false, 401),

        TOKEN_PARSE_ERROR("AUTH_0103", "Failed to parse token", false, 401),

        REFRESH_TOKEN_EXPIRED("AUTH_0104", "Refresh token has expired, please sign in again", false, 401),

        REFRESH_TOKEN_INVALID("AUTH_0105", "Refresh token is invalid, please sign in again", false, 401),

        CODE_SEND_TOO_FREQUENT("AUTH_0200", "Verification code sent too frequently, please try again later", false,
                        429),

        CODE_NOT_FOUND("AUTH_0201", "Verification code not found or expired", false, 400),

        CODE_MISMATCH("AUTH_0202", "Incorrect verification code", false, 400),

        CODE_VERIFICATION_EXCEEDED("AUTH_0203", "Verification attempt limit exceeded, please request a new code", false,
                        429),

        CODE_FORMAT_INVALID("AUTH_0204", "Invalid verification code format", false, 400),

        CODE_SEND_FAILED("AUTH_0205", "Failed to send verification code, please try again later", false, 500),

        CODE_REQUIRED("AUTH_0207", "Verification code is required", false, 400),

        VERIFICATION_CODE_INVALID("AUTH_0206", "Verification code is invalid or expired", false, 400),

        USERNAME_ALREADY_EXISTS("AUTH_0300", "Username already taken", false, 400),

        EMAIL_ALREADY_EXISTS("AUTH_0301", "Email is already registered", false, 400),

        PHONE_ALREADY_EXISTS("AUTH_0302", "Phone number is already registered", false, 400),

        WEAK_PASSWORD("AUTH_0303", "Password is too weak", false, 400),

        INVALID_EMAIL("AUTH_0304", "Invalid email format", false, 400),

        INVALID_PHONE("AUTH_0305", "Invalid phone number format", false, 400),

        UNSUPPORTED_LOGIN_TYPE("AUTH_0306", "Unsupported login type", false, 400),

        UNSUPPORTED_REGISTER_TYPE("AUTH_0307", "Unsupported registration type", false, 400),

        UNSUPPORTED_AUTHENTICATION_TYPE("AUTH_0308", "Unsupported authentication type", false, 400),

        INVALID_REGISTER_REQUEST("AUTH_0309", "Invalid registration request", false, 400),

        INVALID_LOGIN_REQUEST("AUTH_0310", "Invalid login request", false, 400),

        INVALID_USERNAME("AUTH_0311", "Invalid username format", false, 400),

        INVALID_PASSWORD("AUTH_0312", "Invalid password format", false, 400),

        CREDENTIAL_REQUIRED("AUTH_0313", "At least a password or verification code is required", false, 400),

        INVALID_IP_ADDRESS("AUTH_0314", "Invalid IP address format", false, 400),

        PERMISSION_DENIED("AUTH_0400", "Access denied", false, 403),

        ROLE_NOT_FOUND("AUTH_0401", "Role not found", false, 404),

        INVALID_PARAMETER("AUTH_0500", "Invalid parameter", false, 400),

        UNSUPPORTED_USER_DOMAIN("AUTH_0501", "Unsupported user domain", false, 400),

        WECHAT_NOT_CONFIGURED("AUTH_0600", "WeChat login is not configured, please contact your administrator", false,
                        400),

        WECHAT_CODE_INVALID("AUTH_0601", "WeChat authorization code is invalid or expired", false, 400),

        WECHAT_TOKEN_REFRESH_FAILED("AUTH_0602", "WeChat token refresh failed", false, 400),

        WECHAT_API_ERROR("AUTH_0603", "WeChat API returned an error", false, 500),

        OAUTH2_CODE_INVALID("AUTH_0700", "OAuth2 authorization code is invalid or expired", false, 400),

        OAUTH2_TOKEN_MISSING("AUTH_0701", "OAuth2 provider did not return an access token", false, 400),

        OAUTH2_TOKEN_INVALID("AUTH_0702", "OAuth2 token format is invalid", false, 400),

        OAUTH2_REFRESH_FAILED("AUTH_0703", "OAuth2 token refresh failed", false, 400),

        OAUTH2_NOT_CONFIGURED("AUTH_0704", "OAuth2 provider is not configured, please contact your administrator",
                        false, 400),

        SYSTEM_ERROR("AUTH_0900", "System error, please try again later", false, 500);

        private final String code;

        private final String msg;

        private final boolean success;

        private final int httpStatus;
}
