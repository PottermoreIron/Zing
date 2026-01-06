package com.pot.member.service.application.service;

import com.pot.member.service.application.assembler.MemberAssembler;
import com.pot.member.service.application.assembler.PermissionAssembler;
import com.pot.member.service.application.command.ChangePasswordCommand;
import com.pot.member.service.application.command.RegisterMemberCommand;
import com.pot.member.service.application.command.UpdateMemberProfileCommand;
import com.pot.member.service.application.dto.MemberDTO;
import com.pot.member.service.application.dto.PermissionDTO;
import com.pot.member.service.application.query.GetMemberPermissionsQuery;
import com.pot.member.service.application.query.GetMemberQuery;
import com.pot.member.service.domain.model.member.*;
import com.pot.member.service.domain.model.permission.PermissionAggregate;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.service.MemberDomainService;
import com.pot.member.service.domain.service.PermissionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 会员应用服务
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberApplicationService {

    private final MemberRepository memberRepository;
    private final MemberDomainService memberDomainService;
    private final PermissionDomainService permissionDomainService;
    private final MemberAssembler memberAssembler;
    private final PermissionAssembler permissionAssembler;

    /**
     * 注册新会员
     */
    @Transactional
    public MemberDTO register(RegisterMemberCommand command) {
        log.info("注册新会员: {}", command.getEmail());

        // 检查邮箱是否已存在
        Email email = Email.of(command.getEmail());
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已被注册");
        }

        // 检查用户名是否已存在
        Username username = Username.of(command.getUsername());
        if (memberRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已被使用");
        }

        // 如果提供了手机号，检查是否已存在
        if (command.getPhoneNumber() != null && !command.getPhoneNumber().isBlank()) {
            PhoneNumber phoneNumber = PhoneNumber.of(command.getPhoneNumber());
            if (memberRepository.existsByPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("手机号已被注册");
            }
        }

        // 调用领域服务注册
        MemberAggregate member = memberDomainService.register(username, email, command.getPassword());

        // 如果提供了手机号，绑定手机号
        if (command.getPhoneNumber() != null && !command.getPhoneNumber().isBlank()) {
            PhoneNumber phoneNumber = PhoneNumber.of(command.getPhoneNumber());
            memberDomainService.bindPhoneNumber(member, phoneNumber);
            // 重新加载会员信息
            member = memberRepository.findById(member.getMemberId())
                    .orElseThrow(() -> new IllegalStateException("会员注册后未找到"));
        }

        log.info("会员注册成功: memberId={}, email={}", member.getMemberId().value(), email.getValue());
        return memberAssembler.toDTO(member);
    }

    /**
     * 获取会员信息
     */
    public MemberDTO getMember(GetMemberQuery query) {
        MemberAggregate member = null;

        if (query.getMemberId() != null) {
            member = memberRepository.findById(MemberId.of(query.getMemberId()))
                    .orElse(null);
        } else if (query.getEmail() != null) {
            member = memberRepository.findByEmail(Email.of(query.getEmail()))
                    .orElse(null);
        } else if (query.getPhoneNumber() != null) {
            member = memberRepository.findByPhoneNumber(PhoneNumber.of(query.getPhoneNumber()))
                    .orElse(null);
        } else if (query.getUsername() != null) {
            member = memberRepository.findByUsername(Username.of(query.getUsername()))
                    .orElse(null);
        }

        return memberAssembler.toDTO(member);
    }

    /**
     * 更新会员资料
     */
    @Transactional
    public MemberDTO updateProfile(UpdateMemberProfileCommand command) {
        log.info("更新会员资料: memberId={}", command.getMemberId());

        MemberAggregate member = memberRepository.findById(MemberId.of(command.getMemberId()))
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));

        Username newUsername = command.getUsername() != null ? Username.of(command.getUsername()) : null;
        member.updateProfile(newUsername, command.getAvatar(), command.getBio());

        member = memberRepository.save(member);
        return memberAssembler.toDTO(member);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        log.info("修改密码: memberId={}", command.getMemberId());

        MemberAggregate member = memberRepository.findById(MemberId.of(command.getMemberId()))
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));

        memberDomainService.changePassword(
                member,
                command.getOldPassword(),
                command.getNewPassword());
    }

    /**
     * 锁定会员
     */
    @Transactional
    public void lockMember(Long memberId) {
        log.info("锁定会员: memberId={}", memberId);

        MemberAggregate member = memberRepository.findById(MemberId.of(memberId))
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));

        member.lock();
        memberRepository.save(member);
    }

    /**
     * 解锁会员
     */
    @Transactional
    public void unlockMember(Long memberId) {
        log.info("解锁会员: memberId={}", memberId);

        MemberAggregate member = memberRepository.findById(MemberId.of(memberId))
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));

        member.unlock();
        memberRepository.save(member);
    }

    /**
     * 获取会员的所有权限
     */
    public Set<PermissionDTO> getMemberPermissions(GetMemberPermissionsQuery query) {
        log.debug("获取会员权限: memberId={}", query.getMemberId());

        Set<PermissionAggregate> permissions = permissionDomainService.getMemberPermissions(query.getMemberId());
        return permissionAssembler.toDTOSet(permissions);
    }
}
