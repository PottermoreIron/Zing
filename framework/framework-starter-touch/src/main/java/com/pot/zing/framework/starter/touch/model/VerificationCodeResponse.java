package com.pot.zing.framework.starter.touch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result payload for verification-code delivery.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationCodeResponse {

    /**
     * Message identifier.
     */
    private String messageId;

    /**
     * Verification code. This is typically omitted outside test environments.
     */
    private String code;

    /**
     * Expiration timestamp.
     */
    private LocalDateTime expireTime;

    /**
     * Delivery channel.
     */
    private String channelType;
}
