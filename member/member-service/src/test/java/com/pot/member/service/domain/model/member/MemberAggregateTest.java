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

/**
 * {@link MemberAggregate} 单元测试
 */
@DisplayName("MemberAggregate")
class MemberAggregateTest {

    // ========== 工厂 ==========

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("正常注册：状态为 ACTIVE，profile 为空，无角色")
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
            assertThat(member.getMemberId()).isNull(); // 持久化前无 ID
            assertThat(member.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("createFromOAuth2()")
    class CreateFromOAuth2 {

        @Test
        @DisplayName("OAuth2 创建：没有密码哈希，nickname 写入 profile")
        void createFromOAuth2_happyPath() {
            Nickname nickname = Nickname.of("WeChatABC"); // nickname is the display name
            Email email = Email.of("oauth@example.com");

            MemberAggregate member = MemberAggregate.createFromOAuth2(nickname, email, "http://avatar.jpg");

            assertThat(member.getPasswordHash()).isNull();
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.getProfile().getNickname()).isEqualTo("WeChatABC");
        }
    }

    // ========== 状态机 ==========

    @Nested
    @DisplayName("账号状态机")
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
        @DisplayName("ACTIVE → unlock() → 状态不变（无效操作静默忽略）")
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
        @DisplayName("DISABLED → lock() → 抛出 IllegalStateException")
        void lock_fromDisabled_throws() {
            MemberAggregate member = activeMember();
            member.disable();
            assertThatThrownBy(member::lock)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("禁用");
        }
    }

    // ========== 角色管理 ==========

    @Nested
    @DisplayName("角色管理")
    class RoleManagement {

        @Test
        @DisplayName("assignRole() 添加角色 ID")
        void assignRole_addsRoleId() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");
            member.assignRole(10L);
            assertThat(member.getRoleIds()).containsExactly(10L);
        }

        @Test
        @DisplayName("assignRole() 重复添加同一角色，幂等（Set 去重）")
        void assignRole_idempotent() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");
            member.assignRole(10L);
            member.assignRole(10L);
            assertThat(member.getRoleIds()).hasSize(1);
        }

        @Test
        @DisplayName("assignRole() 传入 null → 抛出 IllegalArgumentException")
        void assignRole_nullRoleId_throws() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");
            assertThatThrownBy(() -> member.assignRole(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("revokeRole() 移除角色 ID")
        void revokeRole_removesRoleId() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");
            member.assignRole(10L);
            member.revokeRole(10L);
            assertThat(member.getRoleIds()).isEmpty();
        }

        @Test
        @DisplayName("getRoleIds() 返回不可变视图")
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

    // ========== Profile 更新 ==========

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("更新 profile 后新值生效，updatedAt 刷新")
        void updateProfile_replacesProfile() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");
            LocalDateTime before = member.getUpdatedAt();

            MemberProfile newProfile = MemberProfile.builder().nickname("新昵称").city("上海").build();
            member.updateProfile(newProfile);

            assertThat(member.getProfile().getNickname()).isEqualTo("新昵称");
            assertThat(member.getProfile().getCity()).isEqualTo("上海");
            assertThat(member.getUpdatedAt()).isAfterOrEqualTo(before);
        }
    }

    // ========== 密码更新 ==========

    @Nested
    @DisplayName("updatePassword()")
    class UpdatePassword {

        @Test
        @DisplayName("更新密码哈希成功")
        void updatePassword_success() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "oldHash");
            member.updatePassword("newHash");
            assertThat(member.getPasswordHash()).isEqualTo("newHash");
        }

        @Test
        @DisplayName("空密码哈希 → 抛出 IllegalArgumentException")
        void updatePassword_blank_throws() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");
            assertThatThrownBy(() -> member.updatePassword(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> member.updatePassword(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ========== 领域事件 ==========

    @Nested
    @DisplayName("pullDomainEvents()")
    class DomainEvents {

        @Test
        @DisplayName("pullDomainEvents() 取走事件后清空")
        void pullDomainEvents_clearsAfterPull() {
            MemberAggregate member = MemberAggregate.create(Nickname.of("u1"), Email.of("u1@test.com"), "hash");

            // 用匿名子类注册一个占位事件
            member.registerEvent(new com.pot.member.service.domain.event.MemberDomainEvent("0") {
                @Override
                protected String getEventName() {
                    return "test.event";
                }
            });

            List<com.pot.member.service.domain.event.MemberDomainEvent> events = member.pullDomainEvents();
            assertThat(events).hasSize(1);

            // 再次拉取应为空
            assertThat(member.pullDomainEvents()).isEmpty();
        }
    }
}
