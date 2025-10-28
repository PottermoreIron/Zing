package com.pot.zing.framework.starter.touch.service;

import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import com.pot.zing.framework.starter.touch.model.TouchRequest;
import com.pot.zing.framework.starter.touch.model.TouchResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Pot
 * @created: 2025/10/19 15:28
 * @description: 触达渠道抽象
 */
public interface TouchChannel {
    /**
     * 渠道类型
     */
    TouchChannelType getChannelType();

    /**
     * 发送消息
     */
    R<TouchResponse> send(TouchRequest request);

    /**
     * 批量发送(默认实现为串行发送)
     */
    default R<List<TouchResponse>> batchSend(List<TouchRequest> requests) {
        List<TouchResponse> responses = requests.stream()
                .map(this::send)
                .filter(R::isSuccess)
                .map(R::getData)
                .collect(Collectors.toList());

        return R.success(responses);
    }


    /**
     * 渠道是否可用
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 渠道优先级(数字越小优先级越高)
     */
    default int getPriority() {
        return 100;
    }
}
