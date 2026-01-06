package com.pot.member.service.domain.model.member;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 会员聚合根
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Getter
public class MemberAggregate {

    private MemberId memberId;
    private Username username;
    private Email email;
    private PhoneNumber phoneNumber;
    private String passwordHash;
    private MemberStatus status;
    private String avatar;
    private String bio;
    private Set<Long> roleIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    /**
     * 创建新会员（用于注册）
     */
    public static MemberAggregate create(
            Username username,
            Email email,
            String passwordHash) {
        MemberAggregate member = new MemberAggregate();
        member.username = username;
        member.email = email;
        member.passwordHash = passwordHash;
        member.status = MemberStatus.ACTIVE;
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
            Username username,
            Email email,
            PhoneNumber phoneNumber,
            String passwordHash,
            MemberStatus status,
            String avatar,
            String bio,
            Set<Long> roleIds,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime lastLoginAt) {
        MemberAggregate member = new MemberAggregate();
        member.memberId = memberId;
        member.username = username;
        member.email = email;
        member.phoneNumber = phoneNumber;
        member.passwordHash = passwordHash;
        member.status = status;
        member.avatar = avatar;
        member.bio = bio;
        member.roleIds = roleIds != null ? new HashSet<>(roleIds) : new HashSet<>();
        member.createdAt = createdAt;
        member.updatedAt = updatedAt;
        member.lastLoginAt = lastLoginAt;
        return member;
    }

    /**
     * 更新个人资料
     */
    public void updateProfile(Username username, String avatar, String bio) {
        if (username != null) {
            this.username = username;
        }
        this.avatar = avatar;
        this.bio = bio;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新手机号
     */
    public void updatePhoneNumber(PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新邮箱
     */
    public void updateEmail(Email email) {
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新密码
     */
    public void updatePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("密码哈希不能为空");
        }
        this.passwordHash = newPasswordHash;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 锁定账号
     */
    public void lock() {
        this.status = MemberStatus.LOCKED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 解锁账号
     */
    public void unlock() {
        if (this.status == MemberStatus.LOCKED) {
            this.status = MemberStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 禁用账号
     */
    public void disable() {
        this.status = MemberStatus.DISABLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 启用账号
     */
    public void enable() {
        if (this.status == MemberStatus.DISABLED) {
            this.status = MemberStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 分配角色
     */
    public void assignRole(Long roleId) {
        if (roleId == null || roleId <= 0) {
            throw new IllegalArgumentException("角色ID无效");
        }
        this.roleIds.add(roleId);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 撤销角色
     */
    public void revokeRole(Long roleId) {
        this.roleIds.remove(roleId);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 记录登录时间
     */
    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 检查账号是否可用
     */
    public boolean isAvailable() {
        return status.isActive();
    }

    /**
     * 检查是否拥有某个角色
     */
    public boolean hasRole(Long roleId) {
        return roleIds.contains(roleId);
    }
}
