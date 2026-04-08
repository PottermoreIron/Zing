package com.pot.member.service.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_member")
public class Member implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("gmt_created_at")
    private LocalDateTime gmtCreatedAt;

    @TableField("gmt_updated_at")
    private LocalDateTime gmtUpdatedAt;

    @TableField("gmt_deleted_at")
    private LocalDateTime gmtDeletedAt;

    @TableField("member_id")
    private Long memberId;

    @TableField("nickname")
    private String nickname;

    @TableField("email")
    private String email;

    @TableField("phone")
    private String phone;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("first_name")
    private String firstName;

    @TableField("last_name")
    private String lastName;

    @TableField("gender")
    private Integer gender;

    @TableField("birth")
    private LocalDate birth;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("country_code")
    private String countryCode;

    @TableField("region")
    private String region;

    @TableField("city")
    private String city;

    @TableField("timezone")
    private String timezone;

    @TableField("locale")
    private String locale;

    @TableField("status")
    private String status;

    @TableField("gmt_email_verified_at")
    private LocalDateTime gmtEmailVerifiedAt;

    @TableField("gmt_phone_verified_at")
    private LocalDateTime gmtPhoneVerifiedAt;

    @TableField("gmt_last_login_at")
    private LocalDateTime gmtLastLoginAt;

    @TableField("last_login_ip")
    private String lastLoginIp;

    @TableField("extend_json")
    private String extendJson;

    public Gender getGenderEnum() {
        return this.gender != null ? Gender.fromCode(this.gender) : Gender.UNKNOWN;
    }

    public void setGender(Gender gender) {
        this.gender = gender != null ? gender.getCode() : Gender.UNKNOWN.getCode();
    }

    public AccountStatus getAccountStatusEnum() {
        return this.status != null ? AccountStatus.fromCode(this.status) : null;
    }

    public void setAccountStatus(AccountStatus status) {
        this.status = status != null ? status.getCode() : null;
    }

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return nickname;
        }
        StringBuilder fullName = new StringBuilder();
        if (lastName != null) {
            fullName.append(lastName);
        }
        if (firstName != null) {
            if (!fullName.isEmpty()) {
                fullName.append(" ");
            }
            fullName.append(firstName);
        }
        return fullName.toString();
    }

    public boolean isEmailVerified() {
        return this.gmtEmailVerifiedAt != null;
    }

    public boolean isPhoneVerified() {
        return this.gmtPhoneVerifiedAt != null;
    }

    public boolean isActive() {
        return AccountStatus.ACTIVE.getCode().equals(this.status);
    }

    public boolean isSuspended() {
        return AccountStatus.SUSPENDED.getCode().equals(this.status);
    }

    public void activate() {
        this.status = AccountStatus.ACTIVE.getCode();
    }

    public void suspend() {
        this.status = AccountStatus.SUSPENDED.getCode();
    }

    public void markEmailVerified() {
        this.gmtEmailVerifiedAt = LocalDateTime.now();
    }

    public void markPhoneVerified() {
        this.gmtPhoneVerifiedAt = LocalDateTime.now();
    }

    public void updateLastLogin(String ip) {
        this.gmtLastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ip;
    }

    public String getDisplayName() {
        String fullName = getFullName();
        if (fullName != null && !fullName.trim().isEmpty() && !Objects.equals(fullName, nickname)) {
            return fullName;
        }
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname;
        }
        return email;
    }

    @Getter
    public enum Gender {
        UNKNOWN(0, "未知"),
        MALE(1, "男"),
        FEMALE(2, "女");

        private final Integer code;
        private final String description;

        Gender(Integer code, String description) {
            this.code = code;
            this.description = description;
        }

        public static Gender fromCode(Integer code) {
            for (Gender gender : Gender.values()) {
                if (gender.code.equals(code)) {
                    return gender;
                }
            }
            throw new IllegalArgumentException("未知的性别类型: " + code);
        }
    }

    @Getter
    public enum AccountStatus {
        ACTIVE("active", "活跃"),
        INACTIVE("inactive", "非活跃"),
        SUSPENDED("suspended", "暂停"),
        PENDING_VERIFICATION("pending_verification", "待验证");

        private final String code;
        private final String description;

        AccountStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public static AccountStatus fromCode(String code) {
            for (AccountStatus status : AccountStatus.values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("未知的账户状态: " + code);
        }
    }
}