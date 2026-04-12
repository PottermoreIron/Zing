package com.pot.member.service.application.service;

import com.pot.member.service.application.assembler.MemberAssembler;
import com.pot.member.service.application.command.ChangePasswordCommand;
import com.pot.member.service.application.dto.MemberDTO;
import com.pot.member.service.application.exception.MemberException;
import com.pot.member.service.application.exception.MemberResultCode;
import com.pot.member.service.domain.model.member.Email;
import com.pot.member.service.domain.model.member.MemberAggregate;
import com.pot.member.service.domain.model.member.MemberId;
import com.pot.member.service.domain.model.member.MemberProfile;
import com.pot.member.service.domain.model.member.MemberStatus;
import com.pot.member.service.domain.model.member.Nickname;
import com.pot.member.service.domain.port.DomainEventPublisher;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.service.MemberDomainService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberAccountApplicationService")
class MemberAccountApplicationServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberDomainService memberDomainService;
    @Mock
    private MemberAssembler memberAssembler;
    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private MemberAccountApplicationService service;

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
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("Password change success: delegates to domain service and saves aggregate")
        void changePassword_savesMemberAfterDomainChange() {
            MemberAggregate member = persistedMember(1L);
            ChangePasswordCommand command = new ChangePasswordCommand(1L, "oldPassword", "newPassword");

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
        @DisplayName("Correct password and active account returns DTO")
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
        @DisplayName("User not found throws MemberException")
        void authenticate_memberNotFound_throws() {
            given(memberRepository.findByEmail(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.authenticateWithPassword("nobody@test.com", "pass"))
                    .isInstanceOf(MemberException.class)
                    .extracting("resultCode")
                    .isEqualTo(MemberResultCode.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("Wrong password throws MemberException")
        void authenticate_wrongPassword_throws() {
            MemberAggregate member = persistedMember(1L);
            given(memberRepository.findByEmail(any())).willReturn(Optional.of(member));
            given(memberDomainService.verifyPassword(member, "wrong")).willReturn(false);

            assertThatThrownBy(() -> service.authenticateWithPassword("u1@test.com", "wrong"))
                    .isInstanceOf(MemberException.class)
                    .extracting("resultCode")
                    .isEqualTo(MemberResultCode.PASSWORD_INCORRECT);
        }

        @Test
        @DisplayName("Locked account throws MemberException")
        void authenticate_lockedAccount_throws() {
            MemberAggregate member = persistedMember(1L);
            member.lock();
            given(memberRepository.findByEmail(any())).willReturn(Optional.of(member));
            given(memberDomainService.verifyPassword(member, "pass")).willReturn(true);

            assertThatThrownBy(() -> service.authenticateWithPassword("u1@test.com", "pass"))
                    .isInstanceOf(MemberException.class)
                    .extracting("resultCode")
                    .isEqualTo(MemberResultCode.ACCOUNT_UNAVAILABLE);
        }
    }

    @Nested
    @DisplayName("lockMember() / unlockMember()")
    class LockUnlock {

        @Test
        @DisplayName("lockMember() saves the aggregate")
        void lockMember_savesMember() {
            MemberAggregate member = persistedMember(1L);
            given(memberRepository.findById(MemberId.of(1L))).willReturn(Optional.of(member));
            given(memberRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.lockMember(1L);

            assertThat(member.getStatus()).isEqualTo(MemberStatus.LOCKED);
            then(memberRepository).should().save(member);
        }

        @Test
        @DisplayName("lockMember() throws MemberException when member not found")
        void lockMember_memberNotFound_throws() {
            given(memberRepository.findById(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.lockMember(99L))
                    .isInstanceOf(MemberException.class)
                    .extracting("resultCode")
                    .isEqualTo(MemberResultCode.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("unlockMember() unlocks and saves")
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
    @DisplayName("recordLoginAttempt()")
    class RecordLoginAttempt {

        @Test
        @DisplayName("Successful login updates last login time and saves")
        void recordLoginAttempt_success_savesMember() {
            MemberAggregate member = persistedMember(3L);
            given(memberRepository.findById(MemberId.of(3L))).willReturn(Optional.of(member));

            service.recordLoginAttempt(3L, true, "127.0.0.1", 123L);

            then(memberRepository).should().save(member);
            assertThat(member.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("Failed login does not save member")
        void recordLoginAttempt_failure_doesNotSaveMember() {
            MemberAggregate member = persistedMember(4L);
            given(memberRepository.findById(MemberId.of(4L))).willReturn(Optional.of(member));

            service.recordLoginAttempt(4L, false, "127.0.0.1", 123L);

            then(memberRepository).shouldHaveNoMoreInteractions();
        }
    }
}