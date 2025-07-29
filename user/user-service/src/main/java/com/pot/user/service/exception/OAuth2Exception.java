package com.pot.user.service.exception;

import com.pot.common.enums.ResultCode;

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
