package com.pot.member.service.application.exception;

import com.pot.zing.framework.common.excption.BusinessException;

/**
 * Business exception for member-service use cases.
 */
public class MemberException extends BusinessException {

    public MemberException(MemberResultCode resultCode) {
        super(resultCode);
    }

    public MemberException(MemberResultCode resultCode, String message) {
        super(resultCode, message);
    }
}