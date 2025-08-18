package com.pot.member.service.strategy;

import com.pot.member.service.controller.request.SendCodeRequest;
import com.pot.member.service.enums.SendCodeChannelEnum;

/**
 * @author: Pot
 * @created: 2025/3/27 23:44
 * @description: 发送验证码策略类
 */
public interface SendCodeStrategy {
    /**
     * @param request request
     * @author pot
     * @description
     * @date 23:45 2025/3/27
     **/
    void sendCode(SendCodeRequest request);

    /**
     * @param target target
     * @param code   code
     * @author pot
     * @description
     * @date 23:45 2025/3/27
     **/
    void validateCode(String target, String code);

    /**
     * @return VerificationCodeType
     * @author pot
     * @description
     * @date 23:45 2025/3/27
     **/
    SendCodeChannelEnum getVerificationCodeType();
}
