package com.pot.member.service.application.service;

import com.pot.member.facade.dto.DeviceDTO;
import com.pot.member.facade.dto.MemberProfileDTO;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.member.service.application.assembler.MemberAssembler;
import com.pot.member.service.application.command.ChangePasswordCommand;
import com.pot.member.service.application.command.CreateMemberCommand;
import com.pot.member.service.application.command.RegisterMemberCommand;
import com.pot.member.service.application.command.UpdateMemberProfileCommand;
import com.pot.member.service.application.dto.MemberDTO;
import com.pot.member.service.application.exception.MemberException;
import com.pot.member.service.application.exception.MemberResultCode;
import com.pot.member.service.application.query.GetMemberQuery;
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

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public MemberDTO getMember(GetMemberQuery query) {
        MemberAggregate member = switch (query) {
            case GetMemberQuery.ByMemberId byMemberId ->
                memberRepository.findById(MemberId.of(byMemberId.memberId())).orElse(null);
            case GetMemberQuery.ByEmail byEmail ->
                memberRepository.findByEmail(Email.of(byEmail.email())).orElse(null);
            case GetMemberQuery.ByPhoneNumber byPhoneNumber ->
                memberRepository.findByPhoneNumber(PhoneNumber.of(byPhoneNumber.phoneNumber())).orElse(null);
            case GetMemberQuery.ByNickname byNickname ->
                memberRepository.findByNickname(Nickname.of(byNickname.nickname())).orElse(null);
        };
        return memberAssembler.toDTO(member);
    }

    public MemberDTO findByOAuth2(String provider, String openId) {
        return socialConnectionRepository.findActiveByProviderAndProviderId(provider, openId)
                .flatMap(sc -> memberRepository.findById(MemberId.of(sc.getMemberId())))
                .map(memberAssembler::toDTO)
                .orElse(null);
    }

    public MemberDTO findByWeChat(String weChatOpenId) {
        return socialConnectionRepository.findActiveByMemberIdAndWeChatOpenId(weChatOpenId)
                .flatMap(sc -> memberRepository.findById(MemberId.of(sc.getMemberId())))
                .map(memberAssembler::toDTO)
                .orElse(null);
    }

    public boolean existsByNickname(String nickname) {
        return memberRepository.existsByNickname(Nickname.of(nickname));
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(Email.of(email));
    }

    public boolean existsByPhone(String phone) {
        return memberRepository.existsByPhoneNumber(PhoneNumber.of(phone));
    }

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

    private Optional<MemberAggregate> resolveByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return memberRepository.findByEmail(Email.of(identifier));
        }
        if (identifier.matches("^[0-9+\\-]{8,20}$")) {
            return memberRepository.findByPhoneNumber(PhoneNumber.of(identifier));
        }
        return memberRepository.findByNickname(Nickname.of(identifier));
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

    public MemberProfileDTO getProfile(Long memberId) {
        MemberAggregate member = requireMember(memberId);
        MemberProfile profile = member.getProfile();
        if (profile == null) {
            return null;
        }
        return MemberProfileDTO.builder()
                .memberId(memberId)
                .nickname(profile.getNickname())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .gender(profile.getGender())
                .birthDate(profile.getBirthDate())
                .bio(profile.getBio())
                .countryCode(profile.getCountryCode())
                .region(profile.getRegion())
                .city(profile.getCity())
                .timezone(profile.getTimezone())
                .locale(profile.getLocale())
                .build();
    }

    @Transactional
    public void recordDeviceLogin(Long memberId, DeviceDTO deviceDTO, String ip, String refreshToken) {
        requireMember(memberId);

        Optional<DeviceAggregate> existing = deviceRepository
                .findByMemberIdAndDeviceToken(memberId, deviceDTO.getDeviceToken());

        if (existing.isPresent()) {
            DeviceAggregate device = existing.get();
            device.updateLogin(ip, refreshToken);
            deviceRepository.save(device);
        } else {
            DeviceAggregate device = DeviceAggregate.create(
                    memberId,
                    deviceDTO.getDeviceToken(),
                    deviceDTO.getDeviceType(),
                    deviceDTO.getDeviceName(),
                    deviceDTO.getOsType(),
                    deviceDTO.getOsVersion(),
                    deviceDTO.getAppVersion(),
                    ip, refreshToken);
            deviceRepository.save(device);
        }
    }

    public List<DeviceDTO> getDevices(Long memberId) {
        return deviceRepository.findByMemberId(memberId).stream()
                .map(this::toFacadeDeviceDTO)
                .collect(Collectors.toList());
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
                .findActiveByProviderAndProviderId(request.getProvider(), request.getProviderMemberId());

        if (existing.isPresent()) {
            SocialConnectionAggregate sc = existing.get();
            sc.updateTokens(request.getAccessToken(), request.getRefreshToken(), request.getTokenExpiresAt());
            socialConnectionRepository.save(sc);
        } else {
            SocialConnectionAggregate sc = SocialConnectionAggregate.create(
                    memberId, request.getProvider(), request.getProviderMemberId(),
                    request.getProviderUsername(), request.getProviderEmail(),
                    request.getAccessToken(), request.getRefreshToken(),
                    request.getTokenExpiresAt(), request.getScope(), null);
            socialConnectionRepository.save(sc);
        }
        log.info("绑定社交账号: memberId={}, provider={}", memberId, request.getProvider());
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

    private DeviceDTO toFacadeDeviceDTO(DeviceAggregate device) {
        return DeviceDTO.builder()
                .deviceId(device.getId())
                .memberId(device.getMemberId())
                .deviceToken(device.getDeviceToken())
                .deviceType(device.getDeviceType())
                .deviceName(device.getDeviceName())
                .osType(device.getOsType())
                .osVersion(device.getOsVersion())
                .appVersion(device.getAppVersion())
                .lastLoginIp(device.getLastLoginIp())
                .lastLoginAt(device.getLastLoginAt() != null
                        ? device.getLastLoginAt().toEpochSecond(ZoneOffset.UTC) * 1000
                        : null)
                .refreshToken(device.getRefreshToken())
                .build();
    }
}
