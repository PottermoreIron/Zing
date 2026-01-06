package com.pot.member.service.domain.repository;

import com.pot.member.service.domain.model.member.*;

import java.util.Optional;

/**
 * 会员仓储接口
 * 
 * @author Pot
 * @since 2026-01-06
 */
public interface MemberRepository {

    /**
     * 保存会员
     */
    MemberAggregate save(MemberAggregate member);

    /**
     * 根据ID查找会员
     */
    Optional<MemberAggregate> findById(MemberId memberId);

    /**
     * 根据邮箱查找会员
     */
    Optional<MemberAggregate> findByEmail(Email email);

    /**
     * 根据手机号查找会员
     */
    Optional<MemberAggregate> findByPhoneNumber(PhoneNumber phoneNumber);

    /**
     * 根据用户名查找会员
     */
    Optional<MemberAggregate> findByUsername(Username username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(Email email);

    /**
     * 检查手机号是否存在
     */
    boolean existsByPhoneNumber(PhoneNumber phoneNumber);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(Username username);

    /**
     * 删除会员（软删除）
     */
    void delete(MemberId memberId);
}
