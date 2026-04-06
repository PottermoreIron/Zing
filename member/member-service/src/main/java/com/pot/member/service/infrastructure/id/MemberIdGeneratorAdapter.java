package com.pot.member.service.infrastructure.id;

import com.pot.member.service.domain.port.MemberIdGenerator;
import com.pot.zing.framework.starter.id.service.IdService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
