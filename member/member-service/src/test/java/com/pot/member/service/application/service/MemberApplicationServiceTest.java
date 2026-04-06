package com.pot.member.service.application.service;

import com.pot.member.service.application.assembler.MemberAssembler;
import com.pot.member.service.application.assembler.PermissionAssembler;
import com.pot.member.service.application.command.ChangePasswordCommand;
import com.pot.member.service.application.command.RegisterMemberCommand;
import com.pot.member.service.application.dto.MemberDTO;
import com.pot.member.service.domain.model.member.*;
import com.pot.member.service.domain.model.social.SocialConnectionAggregate;
import com.pot.member.service.domain.port.DomainEventPublisher;
import com.pot.member.service.domain.repository.DeviceRepository;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.repository.RoleRepository;
import com.pot.member.service.domain.repository.SocialConnectionRepository;
import com.pot.member.service.domain.service.MemberDomainService;
import com.pot.member.service.domain.service.PermissionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private RoleRepository roleRepository;
    @Mock
    private MemberDomainService memberDomainService;
    @Mock
    private PermissionDomainService permissionDomainService;
    @Mock
    private MemberAssembler memberAssembler;
    @Mock
    private PermissionAssembler permissionAssembler;
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
        @DisplayName("注册成功：校验唯一性、调用领域服务、保存、返回 DTO")
        void register_happyPath() {
            MemberAggregate created = MemberAggregate.create(
                    Nickname.of("newuser"), Email.of("new@test.com"), "$hash");
            MemberAggregate saved = persistedMember(1L); // 模拟 save 后返回带有 memberId 的聚合
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
        @DisplayName("昵称已存在 → 抛出 IllegalArgumentException")
        void register_duplicateNickname_throws() {
            given(memberRepository.existsByNickname(any())).willReturn(true);

            assertThatThrownBy(() -> service.register(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("昵称");

            then(memberDomainService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("手机号已注册 → 抛出 IllegalArgumentException")
        void register_duplicatePhone_throws() {
            cmd.setPhoneNumber("13800138000");
            given(memberRepository.existsByNickname(any())).willReturn(false);
            given(memberRepository.existsByPhoneNumber(any())).willReturn(true);

            assertThatThrownBy(() -> service.register(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("手机号");

            then(memberDomainService).shouldHaveNoInteractions();
        }
    }


    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("修改密码成功：委托领域服务并保存聚合")
        void changePassword_savesMemberAfterDomainChange() {
            MemberAggregate member = persistedMember(1L);
            ChangePasswordCommand command = ChangePasswordCommand.builder()
                    .memberId(1L)
                    .oldPassword("oldPassword")
                    .newPassword("newPassword")
                    .build();

            given(memberRepository.findById(MemberId.of(1L))).willReturn(Optional.of(member));
            given(memberRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.changePassword(command);

            then(memberDomainService).should().changePassword(member, "oldPassword", "newPassword");
            then(memberRepository).should().save(member);
        }
    }


    @Nested
    @DisplayName("authenticateWithPassword()")
    class AuthenticateWithPassword {

        @Test
        @DisplayName("正确密码 + 账户可用 → 返回 DTO")
        void authenticate_correctPassword_returnsDTO() {
            MemberAggregate member = persistedMember(1L);
            MemberDTO dto = MemberDTO.builder().memberId(1L).build();

            given(memberRepository.findByEmail(any())).willReturn(Optional.of(member));
            given(memberDomainService.verifyPassword(member, "pass123")).willReturn(true);
            given(memberAssembler.toDTO(member)).willReturn(dto);

            MemberDTO result = service.authenticateWithPassword("user@test.com", "pass123");

            assertThat(result).isEqualTo(dto);
        }

        @Test
        @DisplayName("用户不存在 → 抛出 IllegalArgumentException")
        void authenticate_memberNotFound_throws() {
            given(memberRepository.findByEmail(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.authenticateWithPassword("nobody@test.com", "pass"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("密码错误 → 抛出 IllegalArgumentException")
        void authenticate_wrongPassword_throws() {
            MemberAggregate member = persistedMember(1L);
            given(memberRepository.findByEmail(any())).willReturn(Optional.of(member));
            given(memberDomainService.verifyPassword(member, "wrong")).willReturn(false);

            assertThatThrownBy(() -> service.authenticateWithPassword("u1@test.com", "wrong"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("密码");
        }

        @Test
        @DisplayName("账户被锁定 → 抛出 IllegalStateException")
        void authenticate_lockedAccount_throws() {
            MemberAggregate member = persistedMember(1L);
            member.lock(); // ACTIVE → LOCKED
            given(memberRepository.findByEmail(any())).willReturn(Optional.of(member));
            given(memberDomainService.verifyPassword(member, "pass")).willReturn(true);

            assertThatThrownBy(() -> service.authenticateWithPassword("u1@test.com", "pass"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("禁用或锁定");
        }
    }


    @Nested
    @DisplayName("lockMember() / unlockMember()")
    class LockUnlock {

        @Test
        @DisplayName("lockMember() 保存并发布事件")
        void lockMember_savesAndPublishesEvents() {
            MemberAggregate member = persistedMember(1L);
            given(memberRepository.findById(MemberId.of(1L))).willReturn(Optional.of(member));
            given(memberRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.lockMember(1L);

            assertThat(member.getStatus()).isEqualTo(MemberStatus.LOCKED);
            then(memberRepository).should().save(member);
        }

        @Test
        @DisplayName("lockMember() 会员不存在 → 抛出 IllegalArgumentException")
        void lockMember_memberNotFound_throws() {
            given(memberRepository.findById(any())).willReturn(Optional.empty());
            assertThatThrownBy(() -> service.lockMember(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("unlockMember() 解锁并保存")
        void unlockMember_savesAfterUnlock() {
            MemberAggregate member = persistedMember(2L);
            member.lock();
            given(memberRepository.findById(MemberId.of(2L))).willReturn(Optional.of(member));
            given(memberRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.unlockMember(2L);

            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            then(memberRepository).should().save(member);
        }
    }


    @Nested
    @DisplayName("bindOAuth2()")
    class BindOAuth2 {

        @Test
        @DisplayName("新绑定：创建社交连接")
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
        @DisplayName("已有绑定：更新 token")
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
        @DisplayName("新设备：创建设备记录")
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
