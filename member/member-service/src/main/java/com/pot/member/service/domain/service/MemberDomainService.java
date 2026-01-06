package com.pot.member.service.domain.service;

import com.pot.member.service.domain.model.member.Email;
import com.pot.member.service.domain.model.member.MemberAggregate;
import com.pot.member.service.domain.model.member.PhoneNumber;
import com.pot.member.service.domain.model.member.Username;
import com.pot.member.service.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 会员领域服务
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberDomainService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 注册新会员
     */
    public MemberAggregate register(
            Username username,
            Email email,
            String rawPassword) {

        // 检查邮箱是否已存在
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalStateException("邮箱已被注册: " + email.getValue());
        }

        // 加密密码
        String passwordHash = passwordEncoder.encode(rawPassword);

        // 创建会员
        MemberAggregate member = MemberAggregate.create(username, email, passwordHash);

        // 保存会员
        return memberRepository.save(member);
    }

    /**
     * 验证密码
     */
    public boolean verifyPassword(MemberAggregate member, String rawPassword) {
        return passwordEncoder.matches(rawPassword, member.getPasswordHash());
    }

    /**
     * 更改密码
     */
    public void changePassword(
            MemberAggregate member,
            String oldRawPassword,
            String newRawPassword) {

        // 验证旧密码
        if (!verifyPassword(member, oldRawPassword)) {
            throw new IllegalArgumentException("原密码不正确");
        }

        // 加密新密码
        String newPasswordHash = passwordEncoder.encode(newRawPassword);

        // 更新密码
        member.updatePassword(newPasswordHash);

        // 保存
        memberRepository.save(member);
    }

    /**
     * 绑定手机号
     */
    public void bindPhoneNumber(MemberAggregate member, PhoneNumber phoneNumber) {
        // 检查手机号是否已被使用
        if (memberRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalStateException("手机号已被绑定: " + phoneNumber.getValue());
        }

        member.updatePhoneNumber(phoneNumber);
        memberRepository.save(member);
    }

    /**
     * 更换邮箱
     */
    public void changeEmail(MemberAggregate member, Email newEmail) {
        // 检查新邮箱是否已被使用
        if (memberRepository.existsByEmail(newEmail)) {
            throw new IllegalStateException("邮箱已被使用: " + newEmail.getValue());
        }

        member.updateEmail(newEmail);
        memberRepository.save(member);
    }
}
