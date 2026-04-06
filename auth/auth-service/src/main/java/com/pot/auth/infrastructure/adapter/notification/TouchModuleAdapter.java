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
        log.info("[通知] 发送邮件验证码: email={}", email);
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
                log.warn("[通知] 邮件验证码发送失败: email={}, response={}", email, response);
            }
            return success;
        } catch (Exception e) {
            log.error("[通知] 发送邮件验证码异常: email={}", email, e);
            return false;
        }
    }

    @Override
    public boolean sendSmsVerificationCode(String phoneNumber, String code) {
        log.info("[通知] 发送短信验证码: phone={}", phoneNumber);
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
                log.warn("[通知] 短信验证码发送失败: phone={}, response={}", phoneNumber, response);
            }
            return success;
        } catch (Exception e) {
            log.error("[通知] 发送短信验证码异常: phone={}", phoneNumber, e);
            return false;
        }
    }

    @Override
    public boolean sendLoginNotification(String email, String nickname, String ipAddress, String deviceInfo) {
        log.info("[通知] 发送登录通知: email={}, nickname={}", email, nickname);
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
                log.warn("[通知] 登录通知发送失败: email={}", email);
            }
            return success;
        } catch (Exception e) {
            log.error("[通知] 发送登录通知异常: email={}", email, e);
            return false;
        }
    }

    @Override
    public boolean sendAbnormalLoginAlert(String email, String nickname, String ipAddress, String deviceInfo) {
        log.warn("[通知] 发送异地登录告警: email={}, nickname={}, ip={}", email, nickname, ipAddress);
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
                log.error("[通知] 异地登录告警发送失败: email={}, ip={}", email, ipAddress);
            }
            return success;
        } catch (Exception e) {
            log.error("[通知] 发送异地登录告警异常: email={}, ip={}", email, ipAddress, e);
            return false;
        }
    }
}
