package com.pot.member.service.infrastructure.id;

import com.pot.member.service.domain.port.MemberIdGenerator;
import com.pot.zing.framework.starter.id.service.IdService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 会员ID生成器（基于 Snowflake 分布式ID）
 *
 * @author Pot
 * @since 2026-03-22
 */
@Component
@RequiredArgsConstructor
public class MemberIdGeneratorAdapter implements MemberIdGenerator {

    private static final String BIZ_TYPE = "member";

    private final IdService idService;

    @Override
    public Long nextId() {
        return idService.nextId(BIZ_TYPE);
    }
}
