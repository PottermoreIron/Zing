package com.pot.zing.framework.starter.id.service.impl;

import com.pot.zing.framework.starter.id.exception.IdGenerationException;
import com.pot.zing.framework.starter.id.service.IdGenerator;
import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: Pot
 * @created: 2025/10/18 23:24
 * @description: 自定义Leaf分布式Id生成器实现
 */
@Slf4j
@RequiredArgsConstructor
public class LeafIdGeneratorImpl implements IdGenerator {

    private final IDGen idGen;

    @Override
    public Long nextId(String bizType) {
        try {
            Result result = idGen.get(bizType);

            if (result.getStatus() == Status.EXCEPTION) {
                log.error("Failed to generate ID for bizType: {}", bizType);
                throw new IdGenerationException("Failed to generate ID for bizType: " + bizType);
            }

            return result.getId();
        } catch (Exception e) {
            log.error("Generate ID failed, bizType: {}", bizType, e);
            throw new IdGenerationException("ID generation failed", e);
        }
    }

    @Override
    public String getType() {
        return "leaf";
    }
}
