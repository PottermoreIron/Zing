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
import com.pot.member.service.domain.model.member.MemberStatus;
import com.pot.member.service.domain.model.member.Nickname;
import com.pot.member.service.domain.repository.DeviceRepository;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.repository.SocialConnectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberQueryApplicationService")
class MemberQueryApplicationServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private SocialConnectionRepository socialConnectionRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private MemberAssembler memberAssembler;

    @InjectMocks
    private MemberQueryApplicationService service;

    private MemberAggregate memberAggregate(Long memberId) {
        return MemberAggregate.reconstitute(
                MemberId.of(memberId),
                Nickname.of("member" + memberId),
                Email.of("member" + memberId + "@test.com"),
                null,
                "$hash",
                MemberStatus.ACTIVE,
                MemberProfile.builder().nickname("member" + memberId).city("Shanghai").build(),
                Set.of(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null);
    }

    @Nested
    @DisplayName("getMember()")
    class GetMember {

        @Test
        @DisplayName("Query by memberId returns DTO on success")
        void getMember_byMemberId_returnsDto() {
            MemberAggregate aggregate = memberAggregate(1L);
            MemberDTO expected = MemberDTO.builder().memberId(1L).nickname("member1").build();

            given(memberRepository.findById(MemberId.of(1L))).willReturn(Optional.of(aggregate));
            given(memberAssembler.toDTO(aggregate)).willReturn(expected);

            MemberDTO result = service.getMember(GetMemberQuery.byMemberId(1L));

            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("existsBy*")
    class Exists {

        @Test
        @DisplayName("Email existence query delegates directly to repository")
        void existsByEmail_delegatesToRepository() {
            given(memberRepository.existsByEmail(Email.of("member@test.com"))).willReturn(true);

            boolean result = service.existsByEmail("member@test.com");

            assertThat(result).isTrue();
            then(memberRepository).should().existsByEmail(Email.of("member@test.com"));
        }
    }

    @Nested
    @DisplayName("getProfile()")
    class GetProfile {

        @Test
        @DisplayName("Returns profile DTO when member exists")
        void getProfile_memberExists_returnsProfileDto() {
            MemberAggregate aggregate = memberAggregate(2L);
            given(memberRepository.findById(MemberId.of(2L))).willReturn(Optional.of(aggregate));

            MemberProfileDTO result = service.getProfile(2L);

            assertThat(result.memberId()).isEqualTo(2L);
            assertThat(result.nickname()).isEqualTo("member2");
            assertThat(result.city()).isEqualTo("Shanghai");
        }

        @Test
        @DisplayName("Throws MemberException when member not found")
        void getProfile_memberMissing_throwsMemberException() {
            given(memberRepository.findById(MemberId.of(99L))).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProfile(99L))
                    .isInstanceOf(MemberException.class)
                    .extracting("resultCode")
                    .isEqualTo(MemberResultCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getDevices()")
    class GetDevices {

        @Test
        @DisplayName("Device aggregate is mapped to facade DTO")
        void getDevices_mapsAggregatesToFacadeDtos() {
            DeviceAggregate device = DeviceAggregate.reconstitute(
                    10L,
                    3L,
                    "device-token",
                    "MOBILE",
                    "Pixel",
                    "Android",
                    "14",
                    "1.0.0",
                    "127.0.0.1",
                    LocalDateTime.of(2026, 4, 8, 12, 0),
                    "refresh-token",
                    LocalDateTime.now(),
                    LocalDateTime.now());
            given(deviceRepository.findByMemberId(3L)).willReturn(List.of(device));

            List<DeviceDTO> result = service.getDevices(3L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).deviceId()).isEqualTo(10L);
            assertThat(result.get(0).deviceToken()).isEqualTo("device-token");
            assertThat(result.get(0).deviceType()).isEqualTo("MOBILE");
        }
    }
}