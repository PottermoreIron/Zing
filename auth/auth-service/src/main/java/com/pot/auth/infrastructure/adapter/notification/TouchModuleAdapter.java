package com.pot.auth.infrastructure.adapter.notification;

import com.pot.auth.domain.port.NotificationPort;
import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import com.pot.zing.framework.starter.touch.model.TouchRequest;
import com.pot.zing.framework.starter.touch.service.TouchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Notification adapter backed by the touch starter.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TouchModuleAdapter implements NotificationPort {

    private static final String TEMPLATE_EMAIL_CODE = "AUTH_EMAIL_CODE";
    private static final String TEMPLATE_SMS_CODE = "AUTH_SMS_CODE";
    private static final String TEMPLATE_LOGIN_NOTIFY = "AUTH_LOGIN_NOTIFY";
    private static final String TEMPLATE_ABNORMAL_LOGIN = "AUTH_ABNORMAL_LOGIN";

    private final TouchService touchService;

    @Override
    public boolean sendEmailVerificationCode(String email, String code) {
        log.info("[Notification] Sending email verification code — email={}", email);
        try {
            TouchRequest request = TouchRequest.builder()
                    .target(email)
                    .channelType(TouchChannelType.EMAIL)
                    .templateId(TEMPLATE_EMAIL_CODE)
                    .params(Map.of("code", code))
                    .bizType("AUTH_VERIFICATION_CODE")
                    .build();

            var response = touchService.send(request);
            boolean success = response != null && Boolean.TRUE.equals(response.isSuccess());
            if (!success) {
                log.warn("[Notification] Failed to send email code — email={}, response={}", email, response);
            }
            return success;
        } catch (Exception e) {
            log.error("[Notification] Email code send exception — email={}", email, e);
            return false;
        }
    }

    @Override
    public boolean sendSmsVerificationCode(String phoneNumber, String code) {
        log.info("[Notification] Sending SMS verification code — phone={}", phoneNumber);
        try {
            TouchRequest request = TouchRequest.builder()
                    .target(phoneNumber)
                    .channelType(TouchChannelType.SMS)
                    .templateId(TEMPLATE_SMS_CODE)
                    .params(Map.of("code", code))
                    .bizType("AUTH_VERIFICATION_CODE")
                    .build();

            var response = touchService.send(request);
            boolean success = response != null && Boolean.TRUE.equals(response.isSuccess());
            if (!success) {
                log.warn("[Notification] Failed to send SMS code — phone={}, response={}", phoneNumber, response);
            }
            return success;
        } catch (Exception e) {
            log.error("[Notification] SMS code send exception — phone={}", phoneNumber, e);
            return false;
        }
    }

    @Override
    public boolean sendLoginNotification(String email, String nickname, String ipAddress, String deviceInfo) {
        log.info("[Notification] Sending login notification — email={}, nickname={}", email, nickname);
        try {
            TouchRequest request = TouchRequest.builder()
                    .target(email)
                    .channelType(TouchChannelType.EMAIL)
                    .templateId(TEMPLATE_LOGIN_NOTIFY)
                    .params(Map.of(
                            "nickname", nickname,
                            "username", nickname,
                            "ip", ipAddress,
                            "device", deviceInfo))
                    .bizType("AUTH_LOGIN_NOTIFY")
                    .build();

            // Delivery failures must not block the login flow.
            var response = touchService.sendWithFallback(request);
            boolean success = response != null && Boolean.TRUE.equals(response.isSuccess());
            if (!success) {
                log.warn("[Notification] Login notification failed — email={}", email);
            }
            return success;
        } catch (Exception e) {
            log.error("[Notification] Login notification exception — email={}", email, e);
            return false;
        }
    }

    @Override
    public boolean sendAbnormalLoginAlert(String email, String nickname, String ipAddress, String deviceInfo) {
        log.warn("[Notification] Sending unusual-location login alert — email={}, nickname={}, ip={}", email, nickname, ipAddress);
        try {
            TouchRequest request = TouchRequest.builder()
                    .target(email)
                    .channelType(TouchChannelType.EMAIL)
                    .templateId(TEMPLATE_ABNORMAL_LOGIN)
                    .params(Map.of(
                            "nickname", nickname,
                            "username", nickname,
                            "ip", ipAddress,
                            "device", deviceInfo))
                    .bizType("AUTH_ABNORMAL_LOGIN")
                    .build();

            // Abnormal login alerts should still use the fallback channel path.
            var response = touchService.sendWithFallback(request);
            boolean success = response != null && Boolean.TRUE.equals(response.isSuccess());
            if (!success) {
                log.error("[Notification] Unusual-location alert failed — email={}, ip={}", email, ipAddress);
            }
            return success;
        } catch (Exception e) {
            log.error("[Notification] Unusual-location alert exception — email={}, ip={}", email, ipAddress, e);
            return false;
        }
    }
}
