package com.pot.member.service.application.service;

import com.pot.member.service.application.assembler.MemberAssembler;
import com.pot.member.service.application.command.ChangePasswordCommand;
import com.pot.member.service.application.dto.MemberDTO;
import com.pot.member.service.application.exception.MemberException;
import com.pot.member.service.application.exception.MemberResultCode;
import com.pot.member.service.domain.model.member.Email;
import com.pot.member.service.domain.model.member.MemberAggregate;
import com.pot.member.service.domain.model.member.MemberId;
import com.pot.member.service.domain.model.member.Nickname;
import com.pot.member.service.domain.model.member.PhoneNumber;
import com.pot.member.service.domain.port.DomainEventPublisher;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.service.MemberDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAccountApplicationService {

    private final MemberRepository memberRepository;
    private final MemberDomainService memberDomainService;
    private final MemberAssembler memberAssembler;
    private final DomainEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public MemberDTO authenticateWithPassword(String identifier, String rawPassword) {
        MemberAggregate member = resolveByIdentifier(identifier)
                .orElseThrow(() -> new MemberException(MemberResultCode.MEMBER_NOT_FOUND, "用户不存在"));

        if (!memberDomainService.verifyPassword(member, rawPassword)) {
            throw new MemberException(MemberResultCode.PASSWORD_INCORRECT);
        }
        if (!member.isAvailable()) {
            throw new MemberException(MemberResultCode.ACCOUNT_UNAVAILABLE);
        }
        return memberAssembler.toDTO(member);
    }

    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        log.info("修改密码: memberId={}", command.memberId());
        MemberAggregate member = requireMember(command.memberId());
        try {
            memberDomainService.changePassword(member, command.oldPassword(), command.newPassword());
        } catch (IllegalArgumentException ex) {
            throw new MemberException(MemberResultCode.PASSWORD_INCORRECT, ex.getMessage());
        }
        memberRepository.save(member);
    }

    @Transactional
    public void updatePasswordHash(Long memberId, String newPasswordHash) {
        MemberAggregate member = requireMember(memberId);
        member.updatePassword(newPasswordHash);
        memberRepository.save(member);
    }

    @Transactional
    public void lockMember(Long memberId) {
        log.info("锁定会员: memberId={}", memberId);
        MemberAggregate member = requireMember(memberId);
        member.lock();
        memberRepository.save(member);
        publishAndClearEvents(member);
    }

    @Transactional
    public void unlockMember(Long memberId) {
        log.info("解锁会员: memberId={}", memberId);
        MemberAggregate member = requireMember(memberId);
        member.unlock();
        memberRepository.save(member);
    }

    @Transactional
    public void recordLoginAttempt(Long memberId, boolean success, String ip, Long timestamp) {
        MemberAggregate member = requireMember(memberId);
        if (success) {
            member.recordLogin();
            memberRepository.save(member);
        }
        log.debug("记录登录尝试: memberId={}, success={}, ip={}", memberId, success, ip);
    }

    private Optional<MemberAggregate> resolveByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return memberRepository.findByEmail(Email.of(identifier));
        }
        if (identifier.matches("^[0-9+\\-]{8,20}$")) {
            return memberRepository.findByPhoneNumber(PhoneNumber.of(identifier));
        }
        return memberRepository.findByNickname(Nickname.of(identifier));
    }

    private MemberAggregate requireMember(Long memberId) {
        return memberRepository.findById(MemberId.of(memberId))
                .orElseThrow(() -> new MemberException(MemberResultCode.MEMBER_NOT_FOUND, "会员不存在: " + memberId));
    }

    private void publishAndClearEvents(MemberAggregate member) {
        member.pullDomainEvents().forEach(eventPublisher::publish);
    }
}