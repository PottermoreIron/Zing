package com.pot.member.service.application.service;

import com.pot.member.service.application.assembler.MemberAssembler;
import com.pot.member.service.application.command.CreateMemberCommand;
import com.pot.member.service.application.command.RegisterMemberCommand;
import com.pot.member.service.application.dto.MemberDTO;
import com.pot.member.service.application.exception.MemberException;
import com.pot.member.service.application.exception.MemberResultCode;
import com.pot.member.service.domain.model.member.*;
import com.pot.member.service.domain.model.social.SocialConnectionAggregate;
import com.pot.member.service.domain.port.DomainEventPublisher;
import com.pot.member.service.domain.repository.DeviceRepository;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.repository.SocialConnectionRepository;
import com.pot.member.service.domain.service.MemberDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberApplicationService")
class MemberApplicationServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private SocialConnectionRepository socialConnectionRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private MemberDomainService memberDomainService;
    @Mock
    private MemberAssembler memberAssembler;
    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private MemberApplicationService service;

    private MemberAggregate persistedMember(Long id) {
        return MemberAggregate.reconstitute(
                MemberId.of(id),
                Nickname.of("user" + id),
                Email.of("user" + id + "@test.com"),
                null,
                "$2a$10$hash",
                MemberStatus.ACTIVE,
                MemberProfile.empty(),
                Set.of(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null);
    }

    @Nested
    @DisplayName("register()")
    class Register {

        private RegisterMemberCommand cmd;

        @BeforeEach
        void setup() {
            cmd = RegisterMemberCommand.builder()
                    .nickname("newuser")
                    .email("new@test.com")
                    .password("Password1!")
                    .build();
        }

        @Test
        @DisplayName("Registration success: uniqueness checked, domain service called, persisted, DTO returned")
        void register_happyPath() {
            MemberAggregate created = MemberAggregate.create(
                    Nickname.of("newuser"), Email.of("new@test.com"), "$hash");
            MemberAggregate saved = persistedMember(1L); // simulates save returning aggregate with assigned memberId
            MemberDTO expected = MemberDTO.builder().nickname("newuser").build();

            given(memberRepository.existsByNickname(any())).willReturn(false);
            given(memberDomainService.register(any(), any(), any())).willReturn(created);
            given(memberRepository.save(any())).willReturn(saved);
            given(memberAssembler.toDTO(any())).willReturn(expected);

            MemberDTO result = service.register(cmd);

            assertThat(result).isEqualTo(expected);
            then(memberRepository).should().existsByNickname(Nickname.of("newuser"));
            then(memberDomainService).should().register(
                    eq(Nickname.of("newuser")), eq(Email.of("new@test.com")), eq("Password1!"));
            then(memberRepository).should().save(created);
        }

        @Test
        @DisplayName("Duplicate nickname throws MemberException")
        void register_duplicateNickname_throws() {
            given(memberRepository.existsByNickname(any())).willReturn(true);

            assertThatThrownBy(() -> service.register(cmd))
                    .isInstanceOf(MemberException.class)
                    .extracting("resultCode")
                    .isEqualTo(MemberResultCode.NICKNAME_ALREADY_EXISTS);

            then(memberDomainService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Duplicate phone number throws MemberException")
        void register_duplicatePhone_throws() {
            cmd = RegisterMemberCommand.builder()
                    .nickname(cmd.nickname())
                    .email(cmd.email())
                    .password(cmd.password())
                    .phoneNumber("13800138000")
                    .build();
            given(memberRepository.existsByNickname(any())).willReturn(false);
            given(memberRepository.existsByPhoneNumber(any())).willReturn(true);

            assertThatThrownBy(() -> service.register(cmd))
                    .isInstanceOf(MemberException.class)
                    .extracting("resultCode")
                    .isEqualTo(MemberResultCode.PHONE_ALREADY_EXISTS);

            then(memberDomainService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Unique key conflict is converted to MemberException")
        void register_duplicateKeyFromPersistence_throwsMemberException() {
            MemberAggregate created = MemberAggregate.create(
                    Nickname.of("newuser"), Email.of("new@test.com"), "$hash");

            given(memberRepository.existsByNickname(any())).willReturn(false);
            given(memberRepository.existsByEmail(any())).willReturn(false);
            given(memberDomainService.register(any(), any(), any())).willReturn(created);
            given(memberRepository.save(any()))
                    .willThrow(new DuplicateKeyException("Duplicate entry for key 'uk_email'"));

            assertThatThrownBy(() -> service.register(cmd))
                    .isInstanceOf(MemberException.class)
                    .extracting("resultCode")
                    .isEqualTo(MemberResultCode.EMAIL_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("createMember()")
    class CreateMember {

        @Test
        @DisplayName("Nickname and password alone are sufficient to create a member")
        void createMember_nicknameOnly_happyPath() {
            CreateMemberCommand command = CreateMemberCommand.builder()
                    .nickname("nickname_only")
                    .password("Password1!")
                    .build();
            MemberAggregate created = MemberAggregate.create(
                    Nickname.of("nickname_only"), null, "$hash");
            MemberDTO expected = MemberDTO.builder().nickname("nickname_only").build();

            given(memberRepository.existsByNickname(any())).willReturn(false);
            given(memberDomainService.createMember(any(), isNull(), any())).willReturn(created);
            given(memberRepository.save(any())).willAnswer(inv -> {
                MemberAggregate aggregate = inv.getArgument(0);
                aggregate.assignMemberId(MemberId.of(10L));
                return aggregate;
            });
            given(memberAssembler.toDTO(any())).willReturn(expected);

            MemberDTO result = service.createMember(command);

            assertThat(result).isEqualTo(expected);
            then(memberRepository).should().existsByNickname(Nickname.of("nickname_only"));
            then(memberDomainService).should().createMember(eq(Nickname.of("nickname_only")), isNull(),
                    eq("Password1!"));
            then(memberRepository).should().save(argThat(member -> member.getEmail() == null
                    && member.getPhoneNumber() == null));
        }

        @Test
        @DisplayName("Phone-based registration allows a blank email")
        void createMember_phoneOnly_happyPath() {
            CreateMemberCommand command = CreateMemberCommand.builder()
                    .nickname("phone_only")
                    .phoneNumber("13800138000")
                    .password("Password1!")
                    .build();
            MemberAggregate created = MemberAggregate.create(
                    Nickname.of("phone_only"), null, "$hash");
            MemberDTO expected = MemberDTO.builder().nickname("phone_only").phoneNumber("13800138000").build();

            given(memberRepository.existsByNickname(any())).willReturn(false);
            given(memberRepository.existsByPhoneNumber(any())).willReturn(false);
            given(memberDomainService.createMember(any(), isNull(), any())).willReturn(created);
            given(memberRepository.save(any())).willAnswer(inv -> {
                MemberAggregate aggregate = inv.getArgument(0);
                aggregate.assignMemberId(MemberId.of(11L));
                return aggregate;
            });
            given(memberAssembler.toDTO(any())).willReturn(expected);

            MemberDTO result = service.createMember(command);

            assertThat(result).isEqualTo(expected);
            then(memberRepository).should().existsByPhoneNumber(PhoneNumber.of("13800138000"));
            then(memberDomainService).should().createMember(eq(Nickname.of("phone_only")), isNull(), eq("Password1!"));
            then(memberRepository).should().save(argThat(member -> member.getEmail() == null
                    && member.getPhoneNumber().equals(PhoneNumber.of("13800138000"))));
        }
    }

    @Nested
    @DisplayName("bindOAuth2()")
    class BindOAuth2 {

        @Test
        @DisplayName("New binding: creates social connection")
        void bindOAuth2_newBinding_createsSocialConnection() {
            MemberAggregate member = persistedMember(1L);
            given(memberRepository.findById(MemberId.of(1L))).willReturn(Optional.of(member));
            given(socialConnectionRepository.findActiveByProviderAndProviderId("github", "gh123"))
                    .willReturn(Optional.empty());

            com.pot.member.facade.dto.request.BindSocialAccountRequest req = com.pot.member.facade.dto.request.BindSocialAccountRequest
                    .builder()
                    .memberId(1L)
                    .provider("github")
                    .providerMemberId("gh123")
                    .providerUsername("ghuser")
                    .providerEmail("gh@test.com")
                    .accessToken("at")
                    .refreshToken("rt")
                    .tokenExpiresAt(9999999L)
                    .scope("repo")
                    .build();

            service.bindOAuth2(1L, req);

            then(socialConnectionRepository).should().save(any(SocialConnectionAggregate.class));
        }

        @Test
        @DisplayName("Existing binding: updates token")
        void bindOAuth2_existingBinding_updatesTokens() {
            MemberAggregate member = persistedMember(1L);
            SocialConnectionAggregate existing = SocialConnectionAggregate.create(
                    1L, "github", "gh123", "ghuser", "gh@test.com",
                    "old_at", "old_rt", 1000L, "repo", null);

            given(memberRepository.findById(MemberId.of(1L))).willReturn(Optional.of(member));
            given(socialConnectionRepository.findActiveByProviderAndProviderId("github", "gh123"))
                    .willReturn(Optional.of(existing));

            com.pot.member.facade.dto.request.BindSocialAccountRequest req = com.pot.member.facade.dto.request.BindSocialAccountRequest
                    .builder()
                    .provider("github")
                    .providerMemberId("gh123")
                    .accessToken("new_at")
                    .refreshToken("new_rt")
                    .tokenExpiresAt(9999L)
                    .build();

            service.bindOAuth2(1L, req);

            assertThat(existing.getAccessToken()).isEqualTo("new_at");
            then(socialConnectionRepository).should().save(existing);
        }
    }

    @Nested
    @DisplayName("recordDeviceLogin()")
    class RecordDeviceLogin {

        @Test
        @DisplayName("New device: creates device record")
        void recordDeviceLogin_newDevice_creates() {
            MemberAggregate member = persistedMember(1L);
            given(memberRepository.findById(MemberId.of(1L))).willReturn(Optional.of(member));
            given(deviceRepository.findByMemberIdAndDeviceToken(1L, "token123"))
                    .willReturn(Optional.empty());

            com.pot.member.facade.dto.DeviceDTO deviceDTO = com.pot.member.facade.dto.DeviceDTO.builder()
                    .deviceToken("token123")
                    .deviceType("MOBILE")
                    .osType("Android")
                    .appVersion("1.0.0")
                    .build();

            service.recordDeviceLogin(1L, deviceDTO, "127.0.0.1", "refresh_token");

            then(deviceRepository).should()
                    .save(argThat(d -> d.getDeviceToken().equals("token123") && d.getMemberId().equals(1L)));
        }
    }
}
