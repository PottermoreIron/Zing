package com.pot.member.service.application.service;

import com.pot.member.facade.dto.DeviceDTO;
import com.pot.member.facade.dto.MemberProfileDTO;
import com.pot.member.service.application.assembler.MemberAssembler;
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
import com.pot.member.service.domain.repository.DeviceRepository;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.repository.SocialConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryApplicationService {

    private final MemberRepository memberRepository;
    private final SocialConnectionRepository socialConnectionRepository;
    private final DeviceRepository deviceRepository;
    private final MemberAssembler memberAssembler;

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
                .flatMap(connection -> memberRepository.findById(MemberId.of(connection.getMemberId())))
                .map(memberAssembler::toDTO)
                .orElse(null);
    }

    public MemberDTO findByWeChat(String weChatOpenId) {
        return socialConnectionRepository.findActiveByMemberIdAndWeChatOpenId(weChatOpenId)
                .flatMap(connection -> memberRepository.findById(MemberId.of(connection.getMemberId())))
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

    public List<DeviceDTO> getDevices(Long memberId) {
        return deviceRepository.findByMemberId(memberId).stream()
                .map(this::toFacadeDeviceDTO)
                .collect(Collectors.toList());
    }

    private MemberAggregate requireMember(Long memberId) {
        return memberRepository.findById(MemberId.of(memberId))
                .orElseThrow(() -> new MemberException(MemberResultCode.MEMBER_NOT_FOUND, "会员不存在: " + memberId));
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