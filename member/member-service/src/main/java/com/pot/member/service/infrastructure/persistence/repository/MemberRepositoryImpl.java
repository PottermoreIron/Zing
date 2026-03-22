package com.pot.member.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pot.member.service.domain.model.member.*;
import com.pot.member.service.domain.port.MemberIdGenerator;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.infrastructure.persistence.entity.Member;
import com.pot.member.service.infrastructure.persistence.entity.MemberRole;
import com.pot.member.service.infrastructure.persistence.mapper.MemberMapper;
import com.pot.member.service.infrastructure.persistence.mapper.MemberRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final MemberRoleMapper memberRoleMapper;
    private final MemberIdGenerator memberIdGenerator;

    @Override
    public MemberAggregate save(MemberAggregate aggregate) {
        boolean isNew = (aggregate.getMemberId() == null);

        if (isNew) {
            // 新增：预先分配 memberId
            aggregate.assignMemberId(MemberId.of(memberIdGenerator.nextId()));
        }

        Member entity = toEntity(aggregate);

        if (isNew) {
            memberMapper.insert(entity);
            log.debug("新增会员: memberId={}", entity.getMemberId());
        } else {
            LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Member::getMemberId, aggregate.getMemberId().value());
            memberMapper.update(entity, wrapper);
            log.debug("更新会员: memberId={}", entity.getMemberId());
        }

        return toAggregate(entity);
    }

    @Override
    public Optional<MemberAggregate> findById(MemberId memberId) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getMemberId, memberId.value());
        Member entity = memberMapper.selectOne(wrapper);
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
    public Optional<MemberAggregate> findByNickname(Nickname nickname) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getNickname, nickname.getValue());
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
    public boolean existsByNickname(Nickname nickname) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getNickname, nickname.getValue());
        return memberMapper.selectCount(wrapper) > 0;
    }

    @Override
    public void delete(MemberId memberId) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getMemberId, memberId.value());
        memberMapper.delete(wrapper);
        log.debug("删除会员: {}", memberId.value());
    }

    @Override
    public Set<Long> findMemberIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<MemberRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberRole::getRoleId, roleId);
        List<MemberRole> memberRoles = memberRoleMapper.selectList(wrapper);
        return memberRoles.stream()
                .map(MemberRole::getMemberId)
                .collect(Collectors.toSet());
    }

    /**
     * 将实体转换为聚合根
     */
    private MemberAggregate toAggregate(Member entity) {
        MemberProfile profile = MemberProfile.builder()
                .nickname(entity.getNickname())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .gender(entity.getGender())
                .birthDate(entity.getBirth() != null ? entity.getBirth().toString() : null)
                .countryCode(entity.getCountryCode())
                .region(entity.getRegion())
                .city(entity.getCity())
                .timezone(entity.getTimezone())
                .locale(entity.getLocale())
                .build();

        Set<Long> roleIds = loadRoleIds(entity.getMemberId());

        return MemberAggregate.reconstitute(
                MemberId.of(entity.getMemberId()),
                entity.getNickname() != null ? Nickname.of(entity.getNickname()) : null,
                entity.getEmail() != null ? Email.of(entity.getEmail()) : null,
                entity.getPhone() != null ? PhoneNumber.of(entity.getPhone()) : null,
                entity.getPasswordHash(),
                mapStatus(entity.getStatus()),
                profile,
                roleIds,
                entity.getGmtCreatedAt(),
                entity.getGmtUpdatedAt(),
                entity.getGmtLastLoginAt());
    }

    /**
     * 加载会员的角色ID集合
     */
    private Set<Long> loadRoleIds(Long memberId) {
        LambdaQueryWrapper<MemberRole> q = new LambdaQueryWrapper<>();
        q.eq(MemberRole::getMemberId, memberId);
        return memberRoleMapper.selectList(q).stream()
                .map(MemberRole::getRoleId)
                .collect(Collectors.toSet());
    }

    /**
     * 将聚合根转换为实体
     */
    private Member toEntity(MemberAggregate aggregate) {
        Member entity = new Member();
        if (aggregate.getMemberId() != null) {
            // memberId is the business ID — set it so MyBatisPlus UPDATE can find the
            // record via memberId
            entity.setMemberId(aggregate.getMemberId().value());
        }
        entity.setNickname(aggregate.getNickname() != null ? aggregate.getNickname().getValue() : null);
        entity.setEmail(aggregate.getEmail() != null ? aggregate.getEmail().getValue() : null);
        entity.setPhone(aggregate.getPhoneNumber() != null ? aggregate.getPhoneNumber().getValue() : null);
        entity.setPasswordHash(aggregate.getPasswordHash());
        entity.setStatus(mapStatusToString(aggregate.getStatus()));
        entity.setGmtLastLoginAt(aggregate.getLastLoginAt());
        entity.setGmtCreatedAt(
                aggregate.getCreatedAt() != null ? aggregate.getCreatedAt() : java.time.LocalDateTime.now());
        entity.setGmtUpdatedAt(
                aggregate.getUpdatedAt() != null ? aggregate.getUpdatedAt() : java.time.LocalDateTime.now());

        // Profile 字段映射
        MemberProfile profile = aggregate.getProfile();
        if (profile != null) {
            entity.setFirstName(profile.getFirstName());
            entity.setLastName(profile.getLastName());
            entity.setGender(
                    profile.getGender() != null ? Member.Gender.fromCode(profile.getGender()) : Member.Gender.UNKNOWN);
            entity.setBirth(profile.getBirthDate() != null
                    ? java.time.LocalDate.parse(profile.getBirthDate())
                    : null);
            entity.setCountryCode(profile.getCountryCode());
            entity.setRegion(profile.getRegion());
            entity.setCity(profile.getCity());
            entity.setTimezone(profile.getTimezone());
            entity.setLocale(profile.getLocale());
        }
        return entity;
    }

    /**
     * 将数据库状态字符串映射到领域模型状态
     * <p>
     * DB ENUM: active | inactive | suspended | pending_verification
     * </p>
     */
    private MemberStatus mapStatus(String status) {
        if (status == null) {
            return MemberStatus.ACTIVE;
        }
        return switch (status.toLowerCase()) {
            case "active" -> MemberStatus.ACTIVE;
            case "inactive", "pending_verification" -> MemberStatus.DISABLED;
            case "suspended" -> MemberStatus.LOCKED;
            default -> MemberStatus.ACTIVE;
        };
    }

    /**
     * 将领域模型状态映射到数据库状态字符串
     * <p>
     * DB ENUM: active | inactive | suspended | pending_verification
     * </p>
     */
    private String mapStatusToString(MemberStatus status) {
        return switch (status) {
            case ACTIVE -> "active";
            case DISABLED -> "inactive";
            case LOCKED -> "suspended";
        };
    }
}
