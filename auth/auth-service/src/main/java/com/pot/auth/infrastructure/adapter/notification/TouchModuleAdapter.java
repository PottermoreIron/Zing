package com.pot.auth.infrastructure.adapter.notification;

import com.pot.auth.domain.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Touch模块适配器（通知服务）
 *
 * <p>实现NotificationPort接口，调用framework-starter-touch发送通知
 *
 * @author yecao
 * @since 2025-11-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TouchModuleAdapter implements NotificationPort {

    // TODO: 注入Touch模块的Service或Client

    @Override
    public boolean sendEmailVerificationCode(String email, String code) {
        log.info("[通知] 发送邮件验证码: email={}, code={}", email, code);

        try {
            // TODO: 调用framework-starter-touch发送邮件
            // touchService.sendEmail(email, "验证码", "您的验证码是：" + code);

            // 临时实现：仅记录日志
            log.info("[通知] 邮件验证码: {}", code);
            return true;
        } catch (Exception e) {
            log.error("[通知] 发送邮件验证码失败", e);
            return false;
        }
    }

    @Override
    public boolean sendSmsVerificationCode(String phoneNumber, String code) {
        log.info("[通知] 发送短信验证码: phone={}, code={}", phoneNumber, code);

        try {
            // TODO: 调用framework-starter-touch发送短信
            // touchService.sendSms(phoneNumber, "您的验证码是：" + code);

            // 临时实现：仅记录日志
            log.info("[通知] 短信验证码: {}", code);
            return true;
        } catch (Exception e) {
            log.error("[通知] 发送短信验证码失败", e);
            return false;
        }
    }

    @Override
    public boolean sendLoginNotification(String email, String username, String ipAddress, String deviceInfo) {
        log.info("[通知] 发送登录通知: email={}, username={}", email, username);

        try {
            // TODO: 调用framework-starter-touch发送登录通知邮件
            String content = String.format(
                    "您的账号 %s 于 %s 在 %s 设备上登录",
                    username,
                    ipAddress,
                    deviceInfo
            );
            // touchService.sendEmail(email, "登录通知", content);

            log.info("[通知] 登录通知已发送");
            return true;
        } catch (Exception e) {
            log.error("[通知] 发送登录通知失败", e);
            return false;
        }
    }

    @Override
    public boolean sendAbnormalLoginAlert(String email, String username, String ipAddress, String deviceInfo) {
        log.warn("[通知] 发送异地登录告警: email={}, username={}, ip={}", email, username, ipAddress);

        try {
            // TODO: 调用framework-starter-touch发送告警邮件
            String content = String.format(
                    "检测到您的账号 %s 在异地 %s 登录，设备信息：%s。如非本人操作，请立即修改密码。",
                    username,
                    ipAddress,
                    deviceInfo
            );
            // touchService.sendEmail(email, "【安全告警】异地登录", content);

            log.warn("[通知] 异地登录告警已发送");
            return true;
        } catch (Exception e) {
            log.error("[通知] 发送异地登录告警失败", e);
            return false;
        }
    }
}

