package com.pot.member.service.domain.repository;

import com.pot.member.service.domain.model.member.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MemberRepository {

        MemberAggregate save(MemberAggregate member);

        Optional<MemberAggregate> findById(MemberId memberId);

        Optional<MemberAggregate> findByEmail(Email email);

        Optional<MemberAggregate> findByPhoneNumber(PhoneNumber phoneNumber);

        Optional<MemberAggregate> findByNickname(Nickname nickname);

        boolean existsByEmail(Email email);

        boolean existsByPhoneNumber(PhoneNumber phoneNumber);

        boolean existsByNickname(Nickname nickname);

        Set<Long> findMemberIdsByRoleId(Long roleId);

        Map<Long, Set<Long>> findRoleIdsByMemberIds(Set<Long> memberIds);

        void delete(MemberId memberId);
}
