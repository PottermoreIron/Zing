package com.pot.member.service.entity;

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


/**
 * 用户信息实体
 * <p>
 * 用于管理用户的基本信息、认证信息、个人资料等
 * </p>
 *
 * @author Pot
 * @since 2025-09-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_member")
public class Member implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间
     */
    @Null(groups = {Create.class, Update.class}, message = "创建时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_created_at")
    private LocalDateTime gmtCreatedAt;

    /**
     * 更新时间
     */
    @Null(groups = {Create.class, Update.class}, message = "更新时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_updated_at")
    private LocalDateTime gmtUpdatedAt;

    /**
     * 软删除时间
     */
    @Null(groups = {Create.class, Update.class}, message = "删除时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_deleted_at")
    private LocalDateTime gmtDeletedAt;

    /**
     * 用户唯一标识符
     */
    @Null(groups = Create.class, message = "新建用户时ID必须为空")
    @NotNull(groups = Update.class, message = "更新用户时ID不能为空")
    @Positive(groups = Update.class, message = "用户ID必须为正整数")
    @TableField("member_id")
    private Long memberId;

    /**
     * 用户名/昵称
     */
    @NotBlank(groups = Create.class, message = "用户名不能为空")
    @Length(min = 2, max = 50, groups = {Create.class, Update.class}, message = "用户名长度必须在2-50个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9_-]+$",
            groups = {Create.class, Update.class},
            message = "用户名只能包含中文、英文、数字、下划线和横线")
    @TableField("nickname")
    private String nickname;

    /**
     * 邮箱地址
     */
    @NotBlank(groups = Create.class, message = "邮箱地址不能为空")
    @Email(groups = {Create.class, Update.class}, message = "邮箱格式不正确")
    @Length(max = 100, groups = {Create.class, Update.class}, message = "邮箱地址不能超过100个字符")
    @TableField("email")
    private String email;

    /**
     * 手机号码
     */
    @Pattern(regexp = PHONE_REGEX,
            groups = {Create.class, Update.class},
            message = "手机号码格式不正确")
    @TableField("phone")
    private String phone;

    /**
     * 密码哈希值
     */
    @NotBlank(groups = Create.class, message = "密码不能为空")
    @JsonIgnore
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 名
     */
    @Length(max = 50, groups = {Create.class, Update.class}, message = "名不能超过50个字符")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z\\s]*$",
            groups = {Create.class, Update.class},
            message = "名只能包含中文、英文和空格")
    @TableField("first_name")
    private String firstName;

    /**
     * 姓
     */
    @Length(max = 50, groups = {Create.class, Update.class}, message = "姓不能超过50个字符")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z\\s]*$",
            groups = {Create.class, Update.class},
            message = "姓只能包含中文、英文和空格")
    @TableField("last_name")
    private String lastName;

    /**
     * 性别：0-未知，1-男，2-女
     */
    @Min(value = 0, groups = {Create.class, Update.class}, message = "性别值不能小于0")
    @Max(value = 2, groups = {Create.class, Update.class}, message = "性别值不能大于2")
    @TableField("gender")
    private Integer gender;

    /**
     * 出生日期
     */
    @Past(groups = {Create.class, Update.class}, message = "出生日期必须是过去的日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("birth")
    private LocalDate birth;

    /**
     * 头像URL
     */
    @URL(groups = {Create.class, Update.class}, message = "头像URL格式不正确")
    @Length(max = 500, groups = {Create.class, Update.class}, message = "头像URL不能超过500个字符")
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * ISO 3166-1 alpha-2 国家代码
     */
    @Pattern(regexp = "^[A-Z]{2}$",
            groups = {Create.class, Update.class},
            message = "国家代码必须为2位大写字母")
    @TableField("country_code")
    private String countryCode;

    /**
     * 省/州/地区
     */
    @Length(max = 100, groups = {Create.class, Update.class}, message = "地区名称不能超过100个字符")
    @TableField("region")
    private String region;

    /**
     * 城市
     */
    @Length(max = 100, groups = {Create.class, Update.class}, message = "城市名称不能超过100个字符")
    @TableField("city")
    private String city;

    /**
     * 时区
     */
    @Pattern(regexp = "^[A-Za-z_]+/[A-Za-z_]+$",
            groups = {Create.class, Update.class},
            message = "时区格式不正确，应为 Region/City 格式")
    @Length(max = 50, groups = {Create.class, Update.class}, message = "时区不能超过50个字符")
    @TableField("timezone")
    private String timezone;

    /**
     * 语言区域设置
     */
    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$",
            groups = {Create.class, Update.class},
            message = "语言区域设置格式不正确，应为 zh-CN 格式")
    @TableField("locale")
    private String locale;

    /**
     * 账户状态
     */
    @NotBlank(groups = Create.class, message = "账户状态不能为空")
    @Pattern(regexp = "^(ACTIVE|INACTIVE|SUSPENDED|PENDING|DELETED)$",
            groups = {Create.class, Update.class},
            message = "账户状态必须为: ACTIVE, INACTIVE, SUSPENDED, PENDING, DELETED 中的一种")
    @TableField("status")
    private String status;

    /**
     * 邮箱验证时间
     */
    @Null(groups = {Create.class, Update.class}, message = "邮箱验证时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_email_verified_at")
    private LocalDateTime gmtEmailVerifiedAt;

    /**
     * 手机验证时间
     */
    @Null(groups = {Create.class, Update.class}, message = "手机验证时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_phone_verified_at")
    private LocalDateTime gmtPhoneVerifiedAt;

    /**
     * 最后登录时间
     */
    @Null(groups = {Create.class, Update.class}, message = "最后登录时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_last_login_at")
    private LocalDateTime gmtLastLoginAt;

    /**
     * 最后登录IP地址
     */
    @Null(groups = {Create.class, Update.class}, message = "最后登录IP由系统自动生成，不可手动设置")
    @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$",
            message = "IP地址格式不正确")
    @TableField("last_login_ip")
    private String lastLoginIp;

    /**
     * 扩展元数据（JSON格式）
     */
    @Length(max = 2000, groups = {Create.class, Update.class}, message = "扩展数据不能超过2000个字符")
    @TableField("extend_json")
    private String extendJson;

    /**
     * 性别枚举
     */
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

    /**
     * 账户状态枚举
     */
    @Getter
    public enum AccountStatus {
        ACTIVE("ACTIVE", "活跃"),
        INACTIVE("INACTIVE", "非活跃"),
        SUSPENDED("SUSPENDED", "暂停"),
        PENDING("PENDING", "待审核"),
        DELETED("DELETED", "已删除");

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

    /**
     * 业务方法 - 获取性别枚举
     */
    public Gender getGenderEnum() {
        return this.gender != null ? Gender.fromCode(this.gender) : Gender.UNKNOWN;
    }

    /**
     * 业务方法 - 设置性别
     */
    public void setGender(Gender gender) {
        this.gender = gender != null ? gender.getCode() : Gender.UNKNOWN.getCode();
    }

    /**
     * 业务方法 - 获取账户状态枚举
     */
    public AccountStatus getAccountStatusEnum() {
        return this.status != null ? AccountStatus.fromCode(this.status) : null;
    }

    /**
     * 业务方法 - 设置账户状态
     */
    public void setAccountStatus(AccountStatus status) {
        this.status = status != null ? status.getCode() : null;
    }

    /**
     * 业务方法 - 获取完整姓名
     */
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

    /**
     * 业务方法 - 判断邮箱是否已验证
     */
    public boolean isEmailVerified() {
        return this.gmtEmailVerifiedAt != null;
    }

    /**
     * 业务方法 - 判断手机是否已验证
     */
    public boolean isPhoneVerified() {
        return this.gmtPhoneVerifiedAt != null;
    }

    /**
     * 业务方法 - 判断账户是否活跃
     */
    public boolean isActive() {
        return AccountStatus.ACTIVE.getCode().equals(this.status);
    }

    /**
     * 业务方法 - 判断账户是否被暂停
     */
    public boolean isSuspended() {
        return AccountStatus.SUSPENDED.getCode().equals(this.status);
    }

    /**
     * 业务方法 - 设置为活跃状态
     */
    public void activate() {
        this.status = AccountStatus.ACTIVE.getCode();
    }

    /**
     * 业务方法 - 设置为暂停状态
     */
    public void suspend() {
        this.status = AccountStatus.SUSPENDED.getCode();
    }

    /**
     * 业务方法 - 标记邮箱为已验证
     */
    public void markEmailVerified() {
        this.gmtEmailVerifiedAt = LocalDateTime.now();
    }

    /**
     * 业务方法 - 标记手机为已验证
     */
    public void markPhoneVerified() {
        this.gmtPhoneVerifiedAt = LocalDateTime.now();
    }

    /**
     * 业务方法 - 更新最后登录信息
     */
    public void updateLastLogin(String ip) {
        this.gmtLastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ip;
    }

    /**
     * 业务方法 - 获取显示名称（优先级：fullName > nickname > email）
     */
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
}