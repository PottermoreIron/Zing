package com.pot.user.service.service;

import com.pot.common.R;

/**
 * @author: Pot
 * @created: 2025/3/16 22:56
 * @description: 验证码服务接口
 */
public interface SmsCodeService {
    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @author yeyu.cy
     * @description 发送验证码
     * @date 11:42 2025/3/10
     **/
    void sendSmsCode(String phone);

    /**
     * 验证验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return boolean
     * @author yeyu.cy
     * @description 验证验证码
     * @date 11:43 2025/3/10
     **/
    R<Void> validateSmsCode(String phone, String code);
}