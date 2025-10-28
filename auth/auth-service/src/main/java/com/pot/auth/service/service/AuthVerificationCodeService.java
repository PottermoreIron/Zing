package com.pot.auth.service.service;

import com.pot.zing.framework.starter.touch.enums.TouchChannelType;

/**
 * @author: Pot
 * @created: 2025/10/19 18:08
 * @description: 认证验证码服务接口
 */
public interface AuthVerificationCodeService {

    /**
     * 发送注册验证码
     */
    void sendRegisterCode(String target, TouchChannelType sendCodeType);

    /**
     * 发送登录验证码
     */
    void sendLoginCode(String target, TouchChannelType sendCodeType);

    /**
     * 发送重置密码验证码
     */
    void sendResetPasswordCode(String target, TouchChannelType sendCodeType);
}
