package com.pot.member.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pot.member.service.domain.model.member.*;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.entity.Member;
import com.pot.member.service.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Optional;

/**
 * 会员仓储实现
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberMapper memberMapper;

    @Override
    public MemberAggregate save(MemberAggregate aggregate) {
        Member entity = toEntity(aggregate);

        if (entity.getId() == null) {
            // 新增
            memberMapper.insert(entity);
            log.debug("新增会员: {}", entity.getId());
        } else {
            // 更新
            memberMapper.updateById(entity);
            log.debug("更新会员: {}", entity.getId());
        }

        return toAggregate(entity);
    }

    @Override
    public Optional<MemberAggregate> findById(MemberId memberId) {
        Member entity = memberMapper.selectById(memberId.value());
        return Optional.ofNullable(entity).map(this::toAggregate);
    }

    @Override
    public Optional<MemberAggregate> findByEmail(Email email) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getEmail, email.getValue());
        Member entity = memberMapper.selectOne(wrapper);
        return Optional.ofNullable(entity).map(this::toAggregate);
    }

    @Override
    public Optional<MemberAggregate> findByPhoneNumber(PhoneNumber phoneNumber) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getPhone, phoneNumber.getValue());
        Member entity = memberMapper.selectOne(wrapper);
        return Optional.ofNullable(entity).map(this::toAggregate);
    }

    @Override
    public Optional<MemberAggregate> findByUsername(Username username) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getNickname, username.getValue());
        Member entity = memberMapper.selectOne(wrapper);
        return Optional.ofNullable(entity).map(this::toAggregate);
    }

    @Override
    public boolean existsByEmail(Email email) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getEmail, email.getValue());
        return memberMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByPhoneNumber(PhoneNumber phoneNumber) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getPhone, phoneNumber.getValue());
        return memberMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByUsername(Username username) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getNickname, username.getValue());
        return memberMapper.selectCount(wrapper) > 0;
    }

    @Override
    public void delete(MemberId memberId) {
        memberMapper.deleteById(memberId.value());
        log.debug("删除会员: {}", memberId.value());
    }

    /**
     * 将实体转换为聚合根
     */
    private MemberAggregate toAggregate(Member entity) {
        return MemberAggregate.reconstitute(
                MemberId.of(entity.getId()),
                Username.of(entity.getNickname()),
                entity.getEmail() != null ? Email.of(entity.getEmail()) : null,
                entity.getPhone() != null ? PhoneNumber.of(entity.getPhone()) : null,
                entity.getPasswordHash(),
                mapStatus(entity.getStatus()),
                entity.getAvatarUrl(),
                null, // bio - Member entity doesn't have bio field
                new HashSet<>(), // roleIds - loaded separately
                entity.getGmtCreatedAt(),
                entity.getGmtUpdatedAt(),
                entity.getGmtLastLoginAt());
    }

    /**
     * 将聚合根转换为实体
     */
    private Member toEntity(MemberAggregate aggregate) {
        Member entity = new Member();
        if (aggregate.getMemberId() != null) {
            entity.setId(aggregate.getMemberId().value());
        }
        entity.setNickname(aggregate.getUsername().getValue());
        entity.setEmail(aggregate.getEmail() != null ? aggregate.getEmail().getValue() : null);
        entity.setPhone(aggregate.getPhoneNumber() != null ? aggregate.getPhoneNumber().getValue() : null);
        entity.setPasswordHash(aggregate.getPasswordHash());
        entity.setAvatarUrl(aggregate.getAvatar());
        entity.setStatus(mapStatusToString(aggregate.getStatus()));
        entity.setGmtLastLoginAt(aggregate.getLastLoginAt());
        entity.setGmtCreatedAt(aggregate.getCreatedAt());
        entity.setGmtUpdatedAt(aggregate.getUpdatedAt());
        return entity;
    }

    /**
     * 将数据库状态字符串映射到领域模型状态
     */
    private MemberStatus mapStatus(String status) {
        if (status == null) {
            return MemberStatus.ACTIVE;
        }
        return switch (status) {
            case "ACTIVE" -> MemberStatus.ACTIVE;
            case "SUSPENDED", "INACTIVE" -> MemberStatus.DISABLED;
            case "DELETED", "PENDING" -> MemberStatus.LOCKED;
            default -> MemberStatus.ACTIVE;
        };
    }

    /**
     * 将领域模型状态映射到数据库状态字符串
     */
    private String mapStatusToString(MemberStatus status) {
        return switch (status) {
            case ACTIVE -> "ACTIVE";
            case DISABLED -> "SUSPENDED";
            case LOCKED -> "DELETED";
        };
    }
}
