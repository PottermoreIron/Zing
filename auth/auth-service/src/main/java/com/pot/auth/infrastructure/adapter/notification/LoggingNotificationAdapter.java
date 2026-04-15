package com.pot.auth.infrastructure.adapter.notification;

import com.pot.auth.domain.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Dev-only notification stub. Prints verification codes to the log instead of
 * sending real emails or SMS messages. Active only when the "dev" Spring profile
 * is enabled; replaced by {@link TouchModuleAdapter} in other environments.
 */
@Slf4j
@Component
@Profile("dev")
public class LoggingNotificationAdapter implements NotificationPort {

    @Override
    public boolean sendEmailVerificationCode(String email, String code) {
        log.info("[DEV] Email verification code — email={}, code={}", email, code);
        return true;
    }

    @Override
    public boolean sendSmsVerificationCode(String phoneNumber, String code) {
        log.info("[DEV] SMS verification code — phone={}, code={}", phoneNumber, code);
        return true;
    }

    @Override
    public boolean sendLoginNotification(String email, String nickname, String ipAddress, String deviceInfo) {
        log.info("[DEV] Login notification — email={}, nickname={}, ip={}, device={}", email, nickname, ipAddress, deviceInfo);
        return true;
    }

    @Override
    public boolean sendAbnormalLoginAlert(String email, String nickname, String ipAddress, String deviceInfo) {
        log.warn("[DEV] Abnormal login alert — email={}, nickname={}, ip={}, device={}", email, nickname, ipAddress, deviceInfo);
        return true;
    }
}
