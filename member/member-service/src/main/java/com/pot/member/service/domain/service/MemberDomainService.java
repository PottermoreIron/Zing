package com.pot.member.service.domain.service;

import com.pot.member.service.domain.model.member.Email;
import com.pot.member.service.domain.model.member.MemberAggregate;
import com.pot.member.service.domain.model.member.Nickname;
import com.pot.member.service.domain.model.member.PhoneNumber;
import com.pot.member.service.domain.port.PasswordEncoder;
import com.pot.member.service.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Domain service for member lifecycle operations.
 *
 * @author Pot
 * @since 2026-01-06
 */
@Slf4j
@RequiredArgsConstructor
public class MemberDomainService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new member.
     */
    public MemberAggregate register(
            Nickname nickname,
            Email email,
            String rawPassword) {

        if (email == null) {
            throw new IllegalArgumentException("Email must not be blank");
        }

        return createMember(nickname, email, rawPassword);
    }

    /**
     * Create a new member with password credentials.
     */
    public MemberAggregate createMember(
            Nickname nickname,
            Email email,
            String rawPassword) {

        if (email != null && memberRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email already registered: " + email.getValue());
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        return MemberAggregate.create(nickname, email, passwordHash);
    }

    /**
     * Verify a member password.
     */
    public boolean verifyPassword(MemberAggregate member, String rawPassword) {
        return passwordEncoder.matches(rawPassword, member.getPasswordHash());
    }

    /**
     * Change a member password.
     */
    public void changePassword(
            MemberAggregate member,
            String oldRawPassword,
            String newRawPassword) {

        if (!verifyPassword(member, oldRawPassword)) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        String newPasswordHash = passwordEncoder.encode(newRawPassword);

        member.updatePassword(newPasswordHash);
    }

    /**
     * Bind a phone number to a member.
     */
    public void bindPhoneNumber(MemberAggregate member, PhoneNumber phoneNumber) {
        if (memberRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalStateException("Phone number already bound: " + phoneNumber.getValue());
        }

        member.updatePhoneNumber(phoneNumber);
    }

    /**
     * Change the member email.
     */
    public void changeEmail(MemberAggregate member, Email newEmail) {
        if (memberRepository.existsByEmail(newEmail)) {
            throw new IllegalStateException("Email already in use: " + newEmail.getValue());
        }

        member.updateEmail(newEmail);
    }
}
