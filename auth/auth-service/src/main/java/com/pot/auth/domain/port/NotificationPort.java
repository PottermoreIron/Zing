package com.pot.auth.domain.port;

/**
 * 通知端口接口（防腐层）
 *
 * <p>隔离触达服务的具体实现（邮件、短信、推送等）
 *
 * <p>设计原则：
 * <ul>
 *   <li>领域层定义接口（Port）</li>
 *   <li>基础设施层实现适配器（TouchModuleAdapter）</li>
 *   <li>支持多种通知渠道（邮件、短信）</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-10
 */
public interface NotificationPort {

    /**
     * 发送邮件验证码
     *
     * @param email 邮箱地址
     * @param code  验证码
     * @return 是否发送成功
     */
    boolean sendEmailVerificationCode(String email, String code);

    /**
     * 发送短信验证码
     *
     * @param phoneNumber 手机号（国际格式）
     * @param code        验证码
     * @return 是否发送成功
     */
    boolean sendSmsVerificationCode(String phoneNumber, String code);

    /**
     * 发送登录通知邮件
     *
     * @param email      邮箱地址
     * @param username   用户名
     * @param ipAddress  IP地址
     * @param deviceInfo 设备信息
     * @return 是否发送成功
     */
    boolean sendLoginNotification(String email, String username, String ipAddress, String deviceInfo);

    /**
     * 发送异地登录告警
     *
     * @param email      邮箱地址
     * @param username   用户名
     * @param ipAddress  IP地址
     * @param deviceInfo 设备信息
     * @return 是否发送成功
     */
    boolean sendAbnormalLoginAlert(String email, String username, String ipAddress, String deviceInfo);
}

