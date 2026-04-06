package com.pot.member.service.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pot.zing.framework.common.validate.Create;
import com.pot.zing.framework.common.validate.Update;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.pot.zing.framework.common.util.ValidationUtils.PHONE_REGEX;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_member")
public class Member implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    
    @Null(groups = {Create.class, Update.class}, message = "创建时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_created_at")
    private LocalDateTime gmtCreatedAt;

    
    @Null(groups = {Create.class, Update.class}, message = "更新时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_updated_at")
    private LocalDateTime gmtUpdatedAt;

    
    @Null(groups = {Create.class, Update.class}, message = "删除时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_deleted_at")
    private LocalDateTime gmtDeletedAt;

    
    @Null(groups = Create.class, message = "新建用户时ID必须为空")
    @NotNull(groups = Update.class, message = "更新用户时ID不能为空")
    @Positive(groups = Update.class, message = "用户ID必须为正整数")
    @TableField("member_id")
    private Long memberId;

    
    @NotBlank(groups = Create.class, message = "用户名不能为空")
    @Length(min = 2, max = 50, groups = {Create.class, Update.class}, message = "用户名长度必须在2-50个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9_-]+$", groups = {Create.class,
            Update.class}, message = "用户名只能包含中文、英文、数字、下划线和横线")
    @TableField("nickname")
    private String nickname;

    
    @NotBlank(groups = Create.class, message = "邮箱地址不能为空")
    @Email(groups = {Create.class, Update.class}, message = "邮箱格式不正确")
    @Length(max = 100, groups = {Create.class, Update.class}, message = "邮箱地址不能超过100个字符")
    @TableField("email")
    private String email;

    
    @Pattern(regexp = PHONE_REGEX, groups = {Create.class, Update.class}, message = "手机号码格式不正确")
    @TableField("phone")
    private String phone;

    
    @NotBlank(groups = Create.class, message = "密码不能为空")
    @JsonIgnore
    @TableField("password_hash")
    private String passwordHash;

    
    @Length(max = 50, groups = {Create.class, Update.class}, message = "名不能超过50个字符")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z\\s]*$", groups = {Create.class,
            Update.class}, message = "名只能包含中文、英文和空格")
    @TableField("first_name")
    private String firstName;

    
    @Length(max = 50, groups = {Create.class, Update.class}, message = "姓不能超过50个字符")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z\\s]*$", groups = {Create.class,
            Update.class}, message = "姓只能包含中文、英文和空格")
    @TableField("last_name")
    private String lastName;

    
    @Min(value = 0, groups = {Create.class, Update.class}, message = "性别值不能小于0")
    @Max(value = 2, groups = {Create.class, Update.class}, message = "性别值不能大于2")
    @TableField("gender")
    private Integer gender;

    
    @Past(groups = {Create.class, Update.class}, message = "出生日期必须是过去的日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("birth")
    private LocalDate birth;

    
    @URL(groups = {Create.class, Update.class}, message = "头像URL格式不正确")
    @Length(max = 500, groups = {Create.class, Update.class}, message = "头像URL不能超过500个字符")
    @TableField("avatar_url")
    private String avatarUrl;

    
    @Pattern(regexp = "^[A-Z]{2}$", groups = {Create.class, Update.class}, message = "国家代码必须为2位大写字母")
    @TableField("country_code")
    private String countryCode;

    
    @Length(max = 100, groups = {Create.class, Update.class}, message = "地区名称不能超过100个字符")
    @TableField("region")
    private String region;

    
    @Length(max = 100, groups = {Create.class, Update.class}, message = "城市名称不能超过100个字符")
    @TableField("city")
    private String city;

    
    @Pattern(regexp = "^[A-Za-z_]+/[A-Za-z_]+$", groups = {Create.class,
            Update.class}, message = "时区格式不正确，应为 Region/City 格式")
    @Length(max = 50, groups = {Create.class, Update.class}, message = "时区不能超过50个字符")
    @TableField("timezone")
    private String timezone;

    
    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", groups = {Create.class,
            Update.class}, message = "语言区域设置格式不正确，应为 zh-CN 格式")
    @TableField("locale")
    private String locale;

    
    @NotBlank(groups = Create.class, message = "账户状态不能为空")
    @Pattern(regexp = "^(active|inactive|suspended|pending_verification)$", groups = {Create.class,
            Update.class}, message = "账户状态必须为: active, inactive, suspended, pending_verification 中的一种")
    @TableField("status")
    private String status;

    
    @Null(groups = {Create.class, Update.class}, message = "邮箱验证时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_email_verified_at")
    private LocalDateTime gmtEmailVerifiedAt;

    
    @Null(groups = {Create.class, Update.class}, message = "手机验证时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_phone_verified_at")
    private LocalDateTime gmtPhoneVerifiedAt;

    
    @Null(groups = {Create.class, Update.class}, message = "最后登录时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_last_login_at")
    private LocalDateTime gmtLastLoginAt;

    
    @Null(groups = {Create.class, Update.class}, message = "最后登录IP由系统自动生成，不可手动设置")
    @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$", message = "IP地址格式不正确")
    @TableField("last_login_ip")
    private String lastLoginIp;

    
    @Length(max = 2000, groups = {Create.class, Update.class}, message = "扩展数据不能超过2000个字符")
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