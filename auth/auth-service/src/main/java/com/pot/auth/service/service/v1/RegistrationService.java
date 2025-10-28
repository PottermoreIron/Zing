package com.pot.auth.service.service.v1;

import com.pot.auth.service.dto.request.AvailabilityCheckRequest;
import com.pot.auth.service.dto.request.RegistrationRequest;
import com.pot.auth.service.dto.response.AvailabilityCheckResponse;
import com.pot.auth.service.dto.v1.session.AuthSession;

/**
 * 注册服务接口
 * <p>
 * 提供用户注册、可用性检查等功能
 *
 * @author Zing
 * @since 2025-10-26
 */
public interface RegistrationService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 认证会话（自动登录）
     */
    AuthSession register(RegistrationRequest request);

    /**
     * 检查可用性
     *
     * @param request 检查请求
     * @return 可用性检查结果
     */
    AvailabilityCheckResponse checkAvailability(AvailabilityCheckRequest request);

    /**
     * 发送注册验证码
     *
     * @param type      类型（sms/email）
     * @param recipient 接收者
     */
    void sendVerificationCode(String type, String recipient);

    /**
     * 获取注册配置
     *
     * @return 注册配置信息
     */
    Object getRegistrationConfig();
}

