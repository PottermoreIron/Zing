package com.pot.zing.framework.starter.id.service.impl;

import com.pot.zing.framework.starter.id.service.IdGenerator;
import com.pot.zing.framework.starter.id.service.IdService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/10/18 23:27
 * @description: 自定义分布式Id服务
 */
@Service
@RequiredArgsConstructor
public class IdServiceImpl implements IdService {

    private final IdGenerator idGenerator;

    @Override
    public Long nextId(String bizType) {
        return idGenerator.nextId(bizType);
    }
}
