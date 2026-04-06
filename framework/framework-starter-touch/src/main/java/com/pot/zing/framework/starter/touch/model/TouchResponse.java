package com.pot.zing.framework.starter.touch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result payload for a touch send operation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TouchResponse {

    /**
     * Message identifier.
     */
    private String messageId;

    /**
     * Send timestamp.
     */
    private LocalDateTime sendTime;

    /**
     * Channel type.
     */
    private String channelType;
}
