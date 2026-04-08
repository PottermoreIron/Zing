package com.pot.member.service.application.service;

import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.member.service.application.assembler.MemberAssembler;
import com.pot.member.service.application.command.CreateMemberCommand;
import com.pot.member.service.application.command.RegisterMemberCommand;
import com.pot.member.service.application.command.UpdateMemberProfileCommand;
import com.pot.member.service.application.dto.MemberDTO;
import com.pot.member.service.application.exception.MemberException;
import com.pot.member.service.application.exception.MemberResultCode;
import com.pot.member.service.domain.model.device.DeviceAggregate;
import com.pot.member.service.domain.model.member.Email;
import com.pot.member.service.domain.model.member.MemberAggregate;
import com.pot.member.service.domain.model.member.MemberId;
import com.pot.member.service.domain.model.member.MemberProfile;
import com.pot.member.service.domain.model.member.Nickname;
import com.pot.member.service.domain.model.member.PhoneNumber;
import com.pot.member.service.domain.model.social.SocialConnectionAggregate;
import com.pot.member.service.domain.port.DomainEventPublisher;
import com.pot.member.service.domain.repository.DeviceRepository;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.repository.SocialConnectionRepository;
import com.pot.member.service.domain.service.MemberDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Application service that orchestrates member use cases and publishes domain
 * events.
 *
 * @author Pot
 * @since 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberApplicationService {

    private final MemberRepository memberRepository;
    private final SocialConnectionRepository socialConnectionRepository;
    private final DeviceRepository deviceRepository;
    private final MemberDomainService memberDomainService;
    private final MemberAssembler memberAssembler;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public MemberDTO register(RegisterMemberCommand command) {
        log.info("注册新会员: {}", command.email());

        Nickname nickname = requireAvailableNickname(command.nickname());
        PhoneNumber phoneNumber = resolveAvailablePhone(command.phoneNumber());
        Email email = requireAvailableEmail(command.email());

        MemberAggregate member = memberDomainService.register(nickname, email, command.password());

        if (phoneNumber != null) {
            member.updatePhoneNumber(phoneNumber);
        }

        return persistNewMember(member, "会员注册成功");
    }

    @Transactional
    public MemberDTO createMember(CreateMemberCommand command) {
        log.info("内部创建会员: nickname={}", command.nickname());

        Nickname nickname = requireAvailableNickname(command.nickname());
        PhoneNumber phoneNumber = resolveAvailablePhone(command.phoneNumber());
        Email email = resolveAvailableEmail(command.email());

        MemberAggregate member = memberDomainService.createMember(nickname, email, command.password());

        if (phoneNumber != null) {
            member.updatePhoneNumber(phoneNumber);
        }

        return persistNewMember(member, "内部创建会员成功");
    }

    private MemberDTO persistNewMember(MemberAggregate member, String successLogMessage) {
        try {
            member = memberRepository.save(member);
        } catch (DataIntegrityViolationException ex) {
            throw mapRegistrationConflict(ex);
        }
        publishAndClearEvents(member);
        log.info("{}: memberId={}", successLogMessage, member.getMemberId().value());
        return memberAssembler.toDTO(member);
    }

    private Nickname requireAvailableNickname(String nicknameValue) {
        Nickname nickname = Nickname.of(nicknameValue);
        if (memberRepository.existsByNickname(nickname)) {
            throw new MemberException(MemberResultCode.NICKNAME_ALREADY_EXISTS);
        }
        return nickname;
    }

    private PhoneNumber resolveAvailablePhone(String phoneNumberValue) {
        if (phoneNumberValue == null || phoneNumberValue.isBlank()) {
            return null;
        }

        PhoneNumber phoneNumber = PhoneNumber.of(phoneNumberValue);
        if (memberRepository.existsByPhoneNumber(phoneNumber)) {
            throw new MemberException(MemberResultCode.PHONE_ALREADY_EXISTS);
        }
        return phoneNumber;
    }

    private Email requireAvailableEmail(String emailValue) {
        Email email = Email.of(emailValue);
        if (memberRepository.existsByEmail(email)) {
            throw new MemberException(MemberResultCode.EMAIL_ALREADY_EXISTS);
        }
        return email;
    }

    private Email resolveAvailableEmail(String emailValue) {
        if (emailValue == null || emailValue.isBlank()) {
            return null;
        }
        return requireAvailableEmail(emailValue);
    }

    @Transactional
    public MemberDTO createFromOAuth2(String provider, String openId,
            String email, String nickname, String avatarUrl,
            String accessToken, String refreshToken, Long tokenExpiresAt) {
        log.info("OAuth2 创建会员: provider={}, openId={}", provider, openId);

        Email emailVo = email != null ? Email.of(email) : null;
        // Prefer the provider nickname when present; otherwise derive a stable
        // fallback.
        String nicknameStr = (nickname != null && !nickname.isBlank())
                ? nickname
                : (emailVo != null
                        ? emailVo.getValue().split("@")[0] + "_" + System.currentTimeMillis()
                        : provider + "_" + openId.substring(0, Math.min(8, openId.length())));
        Nickname nicknameVo = Nickname.of(nicknameStr);

        MemberAggregate member = MemberAggregate.createFromOAuth2(nicknameVo, emailVo, avatarUrl);
        member = memberRepository.save(member);

        SocialConnectionAggregate social = SocialConnectionAggregate.create(
                member.getMemberId().value(), provider, openId,
                nicknameStr, email, accessToken, refreshToken, tokenExpiresAt, null, null);
        socialConnectionRepository.save(social);

        publishAndClearEvents(member);
        log.info("OAuth2 会员创建成功: memberId={}", member.getMemberId().value());
        return memberAssembler.toDTO(member);
    }

    @Transactional
    public MemberDTO updateProfile(UpdateMemberProfileCommand command) {
        log.info("更新会员资料: memberId={}", command.memberId());
        MemberAggregate member = requireMember(command.memberId());

        MemberProfile newProfile = MemberProfile.builder()
                .nickname(command.nickname())
                .firstName(command.firstName())
                .lastName(command.lastName())
                .gender(command.gender())
                .birthDate(command.birthDate())
                .bio(command.bio())
                .countryCode(command.countryCode())
                .region(command.region())
                .city(command.city())
                .timezone(command.timezone())
                .locale(command.locale())
                .build();

        member.updateProfile(newProfile);
        member = memberRepository.save(member);
        return memberAssembler.toDTO(member);
    }

    @Transactional
    public void recordDeviceLogin(Long memberId, com.pot.member.facade.dto.DeviceDTO deviceDTO, String ip,
            String refreshToken) {
        requireMember(memberId);

        Optional<DeviceAggregate> existing = deviceRepository
                .findByMemberIdAndDeviceToken(memberId, deviceDTO.deviceToken());

        if (existing.isPresent()) {
            DeviceAggregate device = existing.get();
            device.updateLogin(ip, refreshToken);
            deviceRepository.save(device);
        } else {
            DeviceAggregate device = DeviceAggregate.create(
                    memberId,
                    deviceDTO.deviceToken(),
                    deviceDTO.deviceType(),
                    deviceDTO.deviceName(),
                    deviceDTO.osType(),
                    deviceDTO.osVersion(),
                    deviceDTO.appVersion(),
                    ip, refreshToken);
            deviceRepository.save(device);
        }
    }

    @Transactional
    public void kickDevice(Long memberId, Long deviceId) {
        deviceRepository.deleteById(deviceId);
        log.info("踢出设备: memberId={}, deviceId={}", memberId, deviceId);
    }

    @Transactional
    public void bindOAuth2(Long memberId, BindSocialAccountRequest request) {
        requireMember(memberId);

        Optional<SocialConnectionAggregate> existing = socialConnectionRepository
                .findActiveByProviderAndProviderId(request.provider(), request.providerMemberId());

        if (existing.isPresent()) {
            SocialConnectionAggregate sc = existing.get();
            sc.updateTokens(request.accessToken(), request.refreshToken(), request.tokenExpiresAt());
            socialConnectionRepository.save(sc);
        } else {
            SocialConnectionAggregate sc = SocialConnectionAggregate.create(
                    memberId, request.provider(), request.providerMemberId(),
                    request.providerUsername(), request.providerEmail(),
                    request.accessToken(), request.refreshToken(),
                    request.tokenExpiresAt(), request.scope(), null);
            socialConnectionRepository.save(sc);
        }
        log.info("绑定社交账号: memberId={}, provider={}", memberId, request.provider());
    }

    private MemberAggregate requireMember(Long memberId) {
        return memberRepository.findById(MemberId.of(memberId))
                .orElseThrow(() -> new MemberException(MemberResultCode.MEMBER_NOT_FOUND, "会员不存在: " + memberId));
    }

    private MemberException mapRegistrationConflict(RuntimeException ex) {
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("uk_nickname") || message.contains("nickname")) {
                return new MemberException(MemberResultCode.NICKNAME_ALREADY_EXISTS);
            }
            if (message.contains("uk_email") || message.contains("email")) {
                return new MemberException(MemberResultCode.EMAIL_ALREADY_EXISTS);
            }
            if (message.contains("uk_phone") || message.contains("phone")) {
                return new MemberException(MemberResultCode.PHONE_ALREADY_EXISTS);
            }
        }
        return new MemberException(MemberResultCode.REGISTRATION_CONFLICT, ex.getMessage());
    }

    private void publishAndClearEvents(MemberAggregate member) {
        member.pullDomainEvents().forEach(eventPublisher::publish);
    }
}
