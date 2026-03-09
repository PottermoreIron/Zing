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
 * Touch模块适配器（通知服务）
 *
 * <p>
 * 实现 {@link NotificationPort} 接口，通过 {@code framework-starter-touch} 的
 * {@link TouchService} 进行消息路由，解耦业务逻辑与具体发送渠道。
 *
 * <p>
 * 渠道映射：
 * <ul>
 * <li>邮件验证码 → {@link TouchChannelType#EMAIL}，模板 {@code AUTH_EMAIL_CODE}</li>
 * <li>短信验证码 → {@link TouchChannelType#SMS}，模板 {@code AUTH_SMS_CODE}</li>
 * <li>登录通知 → {@link TouchChannelType#EMAIL}，模板 {@code AUTH_LOGIN_NOTIFY}</li>
 * <li>异地登录告警 → {@link TouchChannelType#EMAIL}，模板
 * {@code AUTH_ABNORMAL_LOGIN}</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TouchModuleAdapter implements NotificationPort {

    /** 邮件验证码模板 ID */
    private static final String TEMPLATE_EMAIL_CODE = "AUTH_EMAIL_CODE";
    /** 短信验证码模板 ID */
    private static final String TEMPLATE_SMS_CODE = "AUTH_SMS_CODE";
    /** 登录成功通知模板 ID */
    private static final String TEMPLATE_LOGIN_NOTIFY = "AUTH_LOGIN_NOTIFY";
    /** 异地登录告警模板 ID */
    private static final String TEMPLATE_ABNORMAL_LOGIN = "AUTH_ABNORMAL_LOGIN";

    private final TouchService touchService;

    // ================================================================
    // 验证码发送
    // ================================================================

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

    // ================================================================
    // 安全通知（降级发送，避免影响主流程）
    // ================================================================

    @Override
    public boolean sendLoginNotification(String email, String username, String ipAddress, String deviceInfo) {
        log.info("[通知] 发送登录通知: email={}, username={}", email, username);
        try {
            TouchRequest request = TouchRequest.builder()
                    .target(email)
                    .channelType(TouchChannelType.EMAIL)
                    .templateId(TEMPLATE_LOGIN_NOTIFY)
                    .params(Map.of(
                            "username", username,
                            "ip", ipAddress,
                            "device", deviceInfo))
                    .bizType("AUTH_LOGIN_NOTIFY")
                    .build();

            // 降级发送：网络抖动时不影响登录流程
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
    public boolean sendAbnormalLoginAlert(String email, String username, String ipAddress, String deviceInfo) {
        log.warn("[通知] 发送异地登录告警: email={}, username={}, ip={}", email, username, ipAddress);
        try {
            TouchRequest request = TouchRequest.builder()
                    .target(email)
                    .channelType(TouchChannelType.EMAIL)
                    .templateId(TEMPLATE_ABNORMAL_LOGIN)
                    .params(Map.of(
                            "username", username,
                            "ip", ipAddress,
                            "device", deviceInfo))
                    .bizType("AUTH_ABNORMAL_LOGIN")
                    .build();

            // 异地登录告警优先级高，使用带降级策略的发送
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
