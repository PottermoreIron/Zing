package com.pot.zing.framework.starter.touch.model;

import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request payload for a touch message.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TouchRequest {

    /**
     * Recipient target, such as phone, email, or user ID.
     */
    private String target;

    /**
     * Requested channel type.
     */
    private TouchChannelType channelType;

    /**
     * Template identifier.
     */
    private String templateId;

    /**
     * Template parameters.
     */
    private Map<String, Object> params;

    /**
     * Business type used for auditing.
     */
    private String bizType;
}
