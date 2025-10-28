package com.pot.zing.framework.starter.touch.service;

import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.touch.model.TouchRequest;
import com.pot.zing.framework.starter.touch.model.TouchResponse;

import java.util.List;

/**
 * @author: Pot
 * @created: 2025/10/19 19:56
 * @description: 触达服务接口
 */
public interface TouchService {

    /**
     * 发送消息
     */
    R<TouchResponse> send(TouchRequest request);

    /**
     * 多渠道发送(带降级策略)
     */
    R<TouchResponse> sendWithFallback(TouchRequest request);

    /**
     * 批量发送
     */
    R<List<TouchResponse>> batchSend(List<TouchRequest> requests);
}
