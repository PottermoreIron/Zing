package com.pot.common.id.impl;

import com.pot.common.enums.ResultCode;
import com.pot.common.exception.BusinessException;
import com.pot.common.id.IdService;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Status;
import com.sankuai.inf.leaf.service.SegmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/8/16 22:39
 * @description: 分布式id工具类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeafIdServiceImpl implements IdService {

    private final SegmentService segmentService;

    @Override
    public Long getNextId(String bizType) {
        if (segmentService == null) {
            log.error("segmentService is null");
            throw new BusinessException(ResultCode.GET_ID_EXCEPTION, "segmentService not initialized");
        }

        try {
            log.debug("获取分布式ID，业务类型：{}", bizType);
            Result result = segmentService.getId(bizType);

            if (result.getStatus().equals(Status.EXCEPTION)) {
                log.error("获取分布式ID异常，业务类型：{}", bizType);
                throw new BusinessException(ResultCode.GET_ID_EXCEPTION);
            }

            log.debug("成功获取分布式ID：{}，业务类型：{}", result.getId(), bizType);
            return result.getId();
        } catch (Exception e) {
            log.error("获取分布式id失败，业务类型：{}，错误：{}", bizType, e.getMessage(), e);
            throw new RuntimeException("获取分布式ID失败", e);
        }
    }
}
