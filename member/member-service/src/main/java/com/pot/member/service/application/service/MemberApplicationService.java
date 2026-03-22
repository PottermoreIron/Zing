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
import com.pot.member.service.domain.model.device.DeviceAggregate;
import com.pot.member.service.domain.model.member.*;
import com.pot.member.service.domain.model.permission.PermissionAggregate;
import com.pot.member.service.domain.model.role.RoleAggregate;
import com.pot.member.service.domain.model.role.RoleId;
import com.pot.member.service.domain.model.social.SocialConnection;
import com.pot.member.service.domain.port.DomainEventPublisher;
import com.pot.member.service.domain.repository.DeviceRepository;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.repository.RoleRepository;
import com.pot.member.service.domain.repository.SocialConnectionRepository;
import com.pot.member.service.domain.service.MemberDomainService;
import com.pot.member.service.domain.service.PermissionDomainService;
import com.pot.member.facade.dto.DeviceDTO;
import com.pot.member.facade.dto.MemberProfileDTO;
import com.pot.member.facade.dto.RoleDTO;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 会员应用服务
 *
 * <p>
 * 编排领域服务、仓储、端口完成用例，发布领域事件。
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
    private final RoleRepository roleRepository;
    private final MemberDomainService memberDomainService;
    private final PermissionDomainService permissionDomainService;
    private final MemberAssembler memberAssembler;
    private final PermissionAssembler permissionAssembler;
    private final DomainEventPublisher eventPublisher;

    // ========== 注册 ==========

    /**
     * 注册新会员（密码登录）
     */
    @Transactional
    public MemberDTO register(RegisterMemberCommand command) {
        log.info("注册新会员: {}", command.getEmail());

        Nickname nickname = Nickname.of(command.getNickname());
        if (memberRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("昵称已被使用");
        }

        PhoneNumber phoneNumber = null;
        if (command.getPhoneNumber() != null && !command.getPhoneNumber().isBlank()) {
            phoneNumber = PhoneNumber.of(command.getPhoneNumber());
            if (memberRepository.existsByPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("手机号已被注册");
            }
        }

        Email email = Email.of(command.getEmail());
        MemberAggregate member = memberDomainService.register(nickname, email, command.getPassword());

        if (phoneNumber != null) {
            member.updatePhoneNumber(phoneNumber);
        }

        member = memberRepository.save(member);
        publishAndClearEvents(member);
        log.info("会员注册成功: memberId={}", member.getMemberId().value());
        return memberAssembler.toDTO(member);
    }

    /**
     * 通过 OAuth2 创建会员（首次授权登录时）
     */
    @Transactional
    public MemberDTO createFromOAuth2(String provider, String openId,
            String email, String nickname, String avatarUrl,
            String accessToken, String refreshToken, Long tokenExpiresAt) {
        log.info("OAuth2 创建会员: provider={}, openId={}", provider, openId);

        Email emailVo = email != null ? Email.of(email) : null;
        // prefer OAuth2-provided nickname; if missing, derive one from email or
        // provider+openId
        String nicknameStr = (nickname != null && !nickname.isBlank())
                ? nickname
                : (emailVo != null
                        ? emailVo.getValue().split("@")[0] + "_" + System.currentTimeMillis()
                        : provider + "_" + openId.substring(0, Math.min(8, openId.length())));
        Nickname nicknameVo = Nickname.of(nicknameStr);

        MemberAggregate member = MemberAggregate.createFromOAuth2(nicknameVo, emailVo, avatarUrl);
        member = memberRepository.save(member);

        // 绑定社交账号
        SocialConnection social = SocialConnection.create(
                member.getMemberId().value(), provider, openId,
                nicknameStr, email, accessToken, refreshToken, tokenExpiresAt, null, null);
        socialConnectionRepository.save(social);

        publishAndClearEvents(member);
        log.info("OAuth2 会员创建成功: memberId={}", member.getMemberId().value());
        return memberAssembler.toDTO(member);
    }

    // ========== 查询 ==========

    public MemberDTO getMember(GetMemberQuery query) {
        MemberAggregate member = null;
        if (query.getMemberId() != null) {
            member = memberRepository.findById(MemberId.of(query.getMemberId())).orElse(null);
        } else if (query.getEmail() != null) {
            member = memberRepository.findByEmail(Email.of(query.getEmail())).orElse(null);
        } else if (query.getPhoneNumber() != null) {
            member = memberRepository.findByPhoneNumber(PhoneNumber.of(query.getPhoneNumber())).orElse(null);
        } else if (query.getNickname() != null) {
            member = memberRepository.findByNickname(Nickname.of(query.getNickname())).orElse(null);
        }
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

    // ========== 认证 ==========

    /**
     * 使用密码验证会员身份（identifier 可以是邮箱、手机号或用户名）
     */
    public MemberDTO authenticateWithPassword(String identifier, String rawPassword) {
        MemberAggregate member = resolveByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (!memberDomainService.verifyPassword(member, rawPassword)) {
            throw new IllegalArgumentException("密码不正确");
        }
        if (!member.isAvailable()) {
            throw new IllegalStateException("账户已被禁用或锁定");
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

    // ========== 密码管理 ==========

    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        log.info("修改密码: memberId={}", command.getMemberId());
        MemberAggregate member = requireMember(command.getMemberId());
        memberDomainService.changePassword(member, command.getOldPassword(), command.getNewPassword());
    }

    @Transactional
    public void updatePasswordHash(Long memberId, String newPasswordHash) {
        MemberAggregate member = requireMember(memberId);
        member.updatePassword(newPasswordHash);
        memberRepository.save(member);
    }

    // ========== 账户管理 ==========

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

    // ========== 资料管理 ==========

    @Transactional
    public MemberDTO updateProfile(UpdateMemberProfileCommand command) {
        log.info("更新会员资料: memberId={}", command.getMemberId());
        MemberAggregate member = requireMember(command.getMemberId());

        MemberProfile newProfile = MemberProfile.builder()
                .nickname(command.getNickname())
                .firstName(command.getFirstName())
                .lastName(command.getLastName())
                .gender(command.getGender())
                .birthDate(command.getBirthDate())
                .bio(command.getBio())
                .countryCode(command.getCountryCode())
                .region(command.getRegion())
                .city(command.getCity())
                .timezone(command.getTimezone())
                .locale(command.getLocale())
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
        return com.pot.member.facade.dto.MemberProfileDTO.builder()
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

    // ========== 权限查询 ==========

    public Set<PermissionDTO> getMemberPermissions(GetMemberPermissionsQuery query) {
        Set<PermissionAggregate> permissions = permissionDomainService.getMemberPermissions(query.getMemberId());
        return permissionAssembler.toDTOSet(permissions);
    }

    public Set<String> getPermissionCodes(Long memberId) {
        return permissionDomainService.getMemberPermissions(memberId).stream()
                .map(PermissionAggregate::getPermissionCode)
                .collect(Collectors.toSet());
    }

    public List<RoleDTO> getMemberRoles(Long memberId) {
        MemberAggregate member = requireMember(memberId);
        return roleRepository.findByIds(member.getRoleIds()).stream()
                .map(this::toFacadeRoleDTO)
                .collect(Collectors.toList());
    }

    public java.util.Map<Long, Set<String>> getPermissionsBatch(List<Long> memberIds) {
        java.util.Map<Long, Set<String>> result = new java.util.HashMap<>();
        for (Long memberId : memberIds) {
            try {
                result.put(memberId, getPermissionCodes(memberId));
            } catch (Exception e) {
                log.warn("获取会员{}权限失败: {}", memberId, e.getMessage());
                result.put(memberId, java.util.Collections.emptySet());
            }
        }
        return result;
    }

    // ========== 设备管理 ==========

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

    // ========== OAuth2 / 社交账号 ==========

    @Transactional
    public void bindOAuth2(Long memberId, BindSocialAccountRequest request) {
        requireMember(memberId);

        Optional<SocialConnection> existing = socialConnectionRepository
                .findActiveByProviderAndProviderId(request.getProvider(), request.getProviderMemberId());

        if (existing.isPresent()) {
            SocialConnection sc = existing.get();
            sc.updateTokens(request.getAccessToken(), request.getRefreshToken(), request.getTokenExpiresAt());
            socialConnectionRepository.save(sc);
        } else {
            SocialConnection sc = SocialConnection.create(
                    memberId, request.getProvider(), request.getProviderMemberId(),
                    request.getProviderUsername(), request.getProviderEmail(),
                    request.getAccessToken(), request.getRefreshToken(),
                    request.getTokenExpiresAt(), request.getScope(), null);
            socialConnectionRepository.save(sc);
        }
        log.info("绑定社交账号: memberId={}, provider={}", memberId, request.getProvider());
    }

    // ========== 私有辅助 ==========

    private MemberAggregate requireMember(Long memberId) {
        return memberRepository.findById(MemberId.of(memberId))
                .orElseThrow(() -> new IllegalArgumentException("会员不存在: " + memberId));
    }

    private void publishAndClearEvents(MemberAggregate member) {
        member.pullDomainEvents().forEach(eventPublisher::publish);
    }

    private RoleDTO toFacadeRoleDTO(RoleAggregate role) {
        return com.pot.member.facade.dto.RoleDTO.builder()
                .roleId(role.getRoleId() != null ? role.getRoleId().value() : null)
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName() != null ? role.getRoleName().getValue() : null)
                .description(role.getDescription())
                .build();
    }

    private DeviceDTO toFacadeDeviceDTO(DeviceAggregate d) {
        return DeviceDTO.builder()
                .deviceId(d.getId())
                .memberId(d.getMemberId())
                .deviceToken(d.getDeviceToken())
                .deviceType(d.getDeviceType())
                .deviceName(d.getDeviceName())
                .osType(d.getOsType())
                .osVersion(d.getOsVersion())
                .appVersion(d.getAppVersion())
                .lastLoginIp(d.getLastLoginIp())
                .lastLoginAt(d.getLastLoginAt() != null
                        ? d.getLastLoginAt().toEpochSecond(ZoneOffset.UTC) * 1000
                        : null)
                .refreshToken(d.getRefreshToken())
                .build();
    }
}
