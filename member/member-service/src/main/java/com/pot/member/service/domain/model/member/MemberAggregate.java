package com.pot.member.service.domain.model.member;

import com.pot.member.service.domain.event.MemberDomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 会员聚合根
 *
 * <p>
 * 职责边界：
 * <ul>
 * <li>维护认证相关核心状态（用户名、邮箱、手机、密码、账号状态）</li>
 * <li>维护角色关联</li>
 * <li>Profile 信息通过 {@link MemberProfile} 值对象持有</li>
 * <li>所有状态变更产生领域事件，由基础设施层发布</li>
 * </ul>
 *
 * @author Pot
 * @since 2026-03-18
 */
@Getter
public class MemberAggregate {

    private MemberId memberId;
    private Nickname nickname;
    private Email email;
    private PhoneNumber phoneNumber;
    private String passwordHash;
    private MemberStatus status;
    private MemberProfile profile;
    private Set<Long> roleIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    /** 未发布的领域事件（事务提交后由基础设施发布） */
    private final List<MemberDomainEvent> domainEvents = new ArrayList<>();

    /**
     * 创建新会员（用于注册）
     */
    public static MemberAggregate create(Nickname nickname, Email email, String passwordHash) {
        MemberAggregate member = new MemberAggregate();
        member.nickname = nickname;
        member.email = email;
        member.passwordHash = passwordHash;
        member.status = MemberStatus.ACTIVE;
        member.profile = MemberProfile.empty();
        member.roleIds = new HashSet<>();
        member.createdAt = LocalDateTime.now();
        member.updatedAt = LocalDateTime.now();
        return member;
    }

    /**
     * 通过 OAuth2 创建新会员
     */
    public static MemberAggregate createFromOAuth2(Nickname nickname, Email email,
            String avatarUrl) {
        MemberAggregate member = new MemberAggregate();
        member.nickname = nickname;
        member.email = email;
        member.passwordHash = null;
        member.status = MemberStatus.ACTIVE;
        member.profile = MemberProfile.builder()
                .nickname(nickname.getValue())
                .build();
        member.roleIds = new HashSet<>();
        member.createdAt = LocalDateTime.now();
        member.updatedAt = LocalDateTime.now();
        return member;
    }

    /**
     * 重建会员（从数据库加载）
     */
    public static MemberAggregate reconstitute(
            MemberId memberId,
            Nickname nickname,
            Email email,
            PhoneNumber phoneNumber,
            String passwordHash,
            MemberStatus status,
            MemberProfile profile,
            Set<Long> roleIds,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime lastLoginAt) {
        MemberAggregate member = new MemberAggregate();
        member.memberId = memberId;
        member.nickname = nickname;
        member.email = email;
        member.phoneNumber = phoneNumber;
        member.passwordHash = passwordHash;
        member.status = status;
        member.profile = profile != null ? profile : MemberProfile.empty();
        member.roleIds = roleIds != null ? new HashSet<>(roleIds) : new HashSet<>();
        member.createdAt = createdAt;
        member.updatedAt = updatedAt;
        member.lastLoginAt = lastLoginAt;
        return member;
    }

    // ========== Profile 管理 ==========

    public void updateProfile(MemberProfile newProfile) {
        this.profile = newProfile;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== ID 分配（仅新建时调用一次） ==========

    public void assignMemberId(MemberId memberId) {
        if (this.memberId != null) {
            throw new IllegalStateException("会员ID已分配，不可重复设置");
        }
        this.memberId = memberId;
    }

    // ========== 账号字段更新 ==========

    public void updateNickname(Nickname nickname) {
        this.nickname = nickname;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePhoneNumber(PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateEmail(Email email) {
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("密码哈希不能为空");
        }
        this.passwordHash = newPasswordHash;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 账号状态机 ==========

    public void lock() {
        if (this.status == MemberStatus.DISABLED) {
            throw new IllegalStateException("已禁用的账号无法锁定");
        }
        this.status = MemberStatus.LOCKED;
        this.updatedAt = LocalDateTime.now();
    }

    public void unlock() {
        if (this.status != MemberStatus.LOCKED) {
            return;
        }
        this.status = MemberStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.status = MemberStatus.DISABLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void enable() {
        if (this.status == MemberStatus.DISABLED) {
            this.status = MemberStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
        }
    }

    // ========== 角色管理 ==========

    public void assignRole(Long roleId) {
        if (roleId == null || roleId <= 0) {
            throw new IllegalArgumentException("角色ID无效");
        }
        this.roleIds.add(roleId);
        this.updatedAt = LocalDateTime.now();
    }

    public void revokeRole(Long roleId) {
        this.roleIds.remove(roleId);
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 登录记录 ==========

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // ========== 查询方法 ==========

    public boolean isAvailable() {
        return status.isActive();
    }

    public boolean hasRole(Long roleId) {
        return roleIds.contains(roleId);
    }

    // ========== 领域事件管理 ==========

    public void registerEvent(MemberDomainEvent event) {
        this.domainEvents.add(event);
    }

    /** 取走所有未发布事件（取出即清空） */
    public List<MemberDomainEvent> pullDomainEvents() {
        List<MemberDomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public Set<Long> getRoleIds() {
        return Collections.unmodifiableSet(roleIds);
    }
}
