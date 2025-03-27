package com.pot.user.service.strategy;

import com.pot.user.service.enums.VerificationCodeType;

/**
 * @author: Pot
 * @created: 2025/3/27 23:44
 * @description: 发送验证码策略类
 */
public interface VerificationCodeStrategy {
    /**
     * @param target target
     * @author pot
     * @description
     * @date 23:45 2025/3/27
     **/
    void sendCode(String target);

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
    VerificationCodeType getVerificationCodeType();
}
