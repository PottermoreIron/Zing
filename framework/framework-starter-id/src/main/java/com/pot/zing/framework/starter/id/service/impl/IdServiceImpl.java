package com.pot.zing.framework.starter.id.service.impl;

import com.pot.zing.framework.starter.id.service.IdGenerator;
import com.pot.zing.framework.starter.id.service.IdService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Default distributed ID service implementation.
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
