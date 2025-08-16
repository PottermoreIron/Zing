package com.pot.common.utils;

import com.pot.common.enums.ResultCode;
import com.pot.common.exception.BusinessException;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Status;
import com.sankuai.inf.leaf.service.SegmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/8/16 22:39
 * @description: 分布式id工具类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdUtils {
    private final SegmentService segmentService;

    public Long getNextId(String bizType) {
        try {
            Result result = segmentService.getId(bizType);
            if (result.getStatus().equals(Status.EXCEPTION)) {
                throw new BusinessException(ResultCode.GET_ID_EXCEPTION);
            }
            return result.getId();
        } catch (Exception e) {
            log.error("获取分布式id失败:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
