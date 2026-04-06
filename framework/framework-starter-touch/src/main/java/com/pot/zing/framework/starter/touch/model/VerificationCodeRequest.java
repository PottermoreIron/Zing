package com.pot.zing.framework.starter.touch.model;

import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request payload for sending a verification code.
 */
@Data
@Builder
public class VerificationCodeRequest {

    /**
     * Recipient target, such as phone or email.
     */
    private String target;

    /**
     * Requested channel type.
     */
    private TouchChannelType channelType;

    /**
     * Verification code length.
     */
    @Builder.Default
    private Integer codeLength = 6;

    /**
     * Verification code TTL in seconds.
     */
    @Builder.Default
    private Long expireSeconds = 300L;

    /**
     * Business scenario, such as signup or login.
     */
    private String bizType;

    /**
     * Additional template parameters.
     */
    private Map<String, Object> extraParams;
}
