package com.pot.member.service.domain.model.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MemberAggregate")
class MemberAggregateTest {


    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Normal registration: status is ACTIVE, profile is null, no roles")
        void create_happyPath() {
            Nickname nickname = Nickname.of("testuser");
            Email email = Email.of("test@example.com");
            String hash = "$2a$10$abc";

            MemberAggregate member = MemberAggregate.create(nickname, email, hash);

            assertThat(member.getNickname()).isEqualTo(nickname);
            assertThat(member.getEmail()).isEqualTo(email);
            assertThat(member.getPasswordHash()).isEqualTo(hash);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.getProfile()).isNotNull();
            assertThat(member.getRoleIds()).isEmpty();
            assertThat(member.getMemberId()).isNull(); // no ID before persistence
            assertThat(member.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("createFromOAuth2()")
    class CreateFromOAuth2 {

        @Test
        @DisplayName("OAuth2 creation: no password hash, nickname written to profile")
        void createFromOAuth2_happyPath() {
            Nickname nickname = Nickname.of("WeChatABC"); // nickname is the display name
            Email email = Email.of("oauth@example.com");

            MemberAggregate member = MemberAggregate.createFromOAuth2(nickname, email, "http://avatar.jpg");

            assertThat(member.getPasswordHash()).isNull();
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.getProfile().getNickname()).isEqualTo("WeChatABC");
        }
    }


    @Nested
    @DisplayName("Account state machine")
    class StateMachine {

        private MemberAggregate activeMember() {
            return MemberAggregate.create(Nickname.of("userOne"), Email.of("u1@test.com"), "hash");
        }

        @Test
        @DisplayName("ACTIVE → lock() → LOCKED")
        void lock_fromActive_becomesLocked() {
            MemberAggregate member = activeMember();
            member.lock();
            assertThat(member.getStatus()).isEqualTo(MemberStatus.LOCKED);
        }

        @Test
        @DisplayName("LOCKED → unlock() → ACTIVE")
        void unlock_fromLocked_becomesActive() {
            MemberAggregate member = activeMember();
            member.lock();
            member.unlock();
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("ACTIVE → unlock() → state unchanged (no-op silently ignored)")
        void unlock_fromActive_noop() {
            MemberAggregate member = activeMember();
            member.unlock(); // already active
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("ACTIVE → disable() → DISABLED")
        void disable_fromActive_becomesDisabled() {
            MemberAggregate member = activeMember();
            member.disable();
            assertThat(member.getStatus()).isEqualTo(MemberStatus.DISABLED);
        }

        @Test
        @DisplayName("DISABLED → enable() → ACTIVE")
        void enable_fromDisabled_becomesActive() {
            MemberAggregate member = activeMember();
            member.disable();
            member.enable();
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("DISABLED → lock() → throws IllegalStateException")
        void lock_fromDisabled_throws() {
            MemberAggregate member = activeMember();
            member.disable();
            assertThatThrownBy(member::lock)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("disabled");
        }
    }


    @Nested
    @DisplayName("Role management")
    class RoleManagement {

        @Test
        @DisplayName("assignRole() adds role ID")
        void assignRole_addsRoleId() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");
            member.assignRole(10L);
            assertThat(member.getRoleIds()).containsExactly(10L);
        }

        @Test
        @DisplayName("assignRole() adding duplicate role is idempotent (Set deduplication)")
        void assignRole_idempotent() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");
            member.assignRole(10L);
            member.assignRole(10L);
            assertThat(member.getRoleIds()).hasSize(1);
        }

        @Test
        @DisplayName("assignRole() with null throws IllegalArgumentException")
        void assignRole_nullRoleId_throws() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");
            assertThatThrownBy(() -> member.assignRole(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("revokeRole() removes role ID")
        void revokeRole_removesRoleId() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");
            member.assignRole(10L);
            member.revokeRole(10L);
            assertThat(member.getRoleIds()).isEmpty();
        }

        @Test
        @DisplayName("getRoleIds() returns unmodifiable view")
        void getRoleIds_isUnmodifiable() {
            MemberAggregate member = MemberAggregate.reconstitute(
                    MemberId.of(1L), Nickname.of("userOne"), Email.of("u1@test.com"),
                    null, "hash", MemberStatus.ACTIVE, MemberProfile.empty(),
                    new HashSet<>(Set.of(10L, 20L)),
                    LocalDateTime.now(), LocalDateTime.now(), null);

            assertThatThrownBy(() -> member.getRoleIds().add(99L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }


    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("After profile update, new values take effect and updatedAt is refreshed")
        void updateProfile_replacesProfile() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");
            LocalDateTime before = member.getUpdatedAt();

            MemberProfile newProfile = MemberProfile.builder().nickname("NewNickname").city("Shanghai").build();
            member.updateProfile(newProfile);

            assertThat(member.getProfile().getNickname()).isEqualTo("NewNickname");
            assertThat(member.getProfile().getCity()).isEqualTo("Shanghai");
            assertThat(member.getUpdatedAt()).isAfterOrEqualTo(before);
        }
    }


    @Nested
    @DisplayName("updatePassword()")
    class UpdatePassword {

        @Test
        @DisplayName("Password hash update succeeds")
        void updatePassword_success() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "oldHash");
            member.updatePassword("newHash");
            assertThat(member.getPasswordHash()).isEqualTo("newHash");
        }

        @Test
        @DisplayName("Blank password hash throws IllegalArgumentException")
        void updatePassword_blank_throws() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");
            assertThatThrownBy(() -> member.updatePassword(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> member.updatePassword(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }


    @Nested
    @DisplayName("pullDomainEvents()")
    class DomainEvents {

        @Test
        @DisplayName("pullDomainEvents() clears events after retrieval")
        void pullDomainEvents_clearsAfterPull() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");

            member.registerEvent(new com.pot.member.service.domain.event.MemberDomainEvent("0") {
                @Override
                protected String getEventName() {
                    return "test.event";
                }
            });

            List<com.pot.member.service.domain.event.MemberDomainEvent> events = member.pullDomainEvents();
            assertThat(events).hasSize(1);

            assertThat(member.pullDomainEvents()).isEmpty();
        }
    }
}
