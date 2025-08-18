package com.pot.member.service.exception;

import com.pot.common.enums.ResultCode;
import com.pot.common.exception.BusinessException;

/**
 * @author: Pot
 * @created: 2025/4/6 17:11
 * @description: Oauth2异常
 */
public class OAuth2Exception extends BusinessException {
    public OAuth2Exception(ResultCode resultCode) {
        super(resultCode);
    }

    public OAuth2Exception(ResultCode resultCode, String message) {
        super(resultCode, message);
    }
}
