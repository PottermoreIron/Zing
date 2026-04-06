package com.pot.auth.domain.port;

/**
 * Port for outbound notifications used by auth flows.
 */
public interface NotificationPort {

    /**
     * Sends an email verification code.
     */
    boolean sendEmailVerificationCode(String email, String code);

    /**
     * Sends an SMS verification code.
     */
    boolean sendSmsVerificationCode(String phoneNumber, String code);

    /**
     * Sends a login notification.
     */
    boolean sendLoginNotification(String email, String nickname, String ipAddress, String deviceInfo);

    /**
     * Sends an abnormal-login alert.
     */
    boolean sendAbnormalLoginAlert(String email, String nickname, String ipAddress, String deviceInfo);
}
