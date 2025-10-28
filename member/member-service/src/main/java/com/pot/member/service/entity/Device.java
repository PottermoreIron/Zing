package com.pot.member.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.pot.zing.framework.common.validate.Create;
import com.pot.zing.framework.common.validate.Update;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户设备信息实体
 * <p>
 * 用于管理用户的设备信息，包括设备识别、平台信息、推送配置等
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_device")
public class Device implements Serializable {

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
     * 用户ID
     */
    @NotNull(groups = {Create.class, Update.class}, message = "用户ID不能为空")
    @Positive(groups = {Create.class, Update.class}, message = "用户ID必须为正整数")
    @TableField("member_id")
    private Long memberId;

    /**
     * 设备唯一标识
     */
    @NotBlank(groups = {Create.class, Update.class}, message = "设备ID不能为空")
    @Length(min = 8, max = 128, groups = {Create.class, Update.class}, message = "设备ID长度必须在8-128个字符之间")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", groups = {Create.class, Update.class}, message = "设备ID只能包含字母、数字、下划线和横线")
    @TableField("device_id")
    private String deviceId;

    /**
     * 设备类型
     */
    @NotBlank(groups = Create.class, message = "新建设备时设备类型不能为空")
    @Pattern(regexp = "^(MOBILE|TABLET|DESKTOP|TV|WATCH|OTHER)$",
            groups = {Create.class, Update.class},
            message = "设备类型必须为: MOBILE, TABLET, DESKTOP, TV, WATCH, OTHER 中的一种")
    @TableField("device_type")
    private String deviceType;

    /**
     * 操作系统平台
     */
    @NotBlank(groups = Create.class, message = "新建设备时操作系统平台不能为空")
    @Length(max = 50, groups = {Create.class, Update.class}, message = "操作系统平台名称不能超过50个字符")
    @Pattern(regexp = "^(iOS|Android|Windows|macOS|Linux|Other)$",
            groups = {Create.class, Update.class},
            message = "操作系统平台必须为: iOS, Android, Windows, macOS, Linux, Other 中的一种")
    @TableField("platform")
    private String platform;

    /**
     * 浏览器信息
     */
    @Length(max = 200, groups = {Create.class, Update.class}, message = "浏览器信息不能超过200个字符")
    @TableField("browser")
    private String browser;

    /**
     * 应用版本
     */
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", groups = {Create.class, Update.class}, message = "应用版本格式必须为 x.y.z 的形式")
    @Length(max = 20, groups = {Create.class, Update.class}, message = "应用版本不能超过20个字符")
    @TableField("app_version")
    private String appVersion;

    /**
     * 推送令牌
     */
    @Length(max = 512, groups = {Create.class, Update.class}, message = "推送令牌不能超过512个字符")
    @TableField("push_token")
    private String pushToken;

    /**
     * 是否活跃设备 (0-非活跃, 1-活跃)
     */
    @NotNull(groups = Create.class, message = "新建设备时活跃状态不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "设备活跃状态值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "设备活跃状态值不能大于1")
    @TableField("is_active")
    private Integer isActive;

    /**
     * 最后使用时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_last_used_at")
    private LocalDateTime gmtLastUsedAt;

    /**
     * 设备类型枚举
     */
    @Getter
    public enum DeviceType {
        MOBILE("MOBILE", "移动设备"),
        TABLET("TABLET", "平板设备"),
        DESKTOP("DESKTOP", "桌面设备"),
        TV("TV", "电视设备"),
        WATCH("WATCH", "手表设备"),
        OTHER("OTHER", "其他设备");

        private final String code;
        private final String description;

        DeviceType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public static DeviceType fromCode(String code) {
            for (DeviceType type : DeviceType.values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("未知的设备类型: " + code);
        }
    }

    /**
     * 平台类型枚举
     */
    @Getter
    public enum Platform {
        IOS("iOS", "苹果iOS系统"),
        ANDROID("Android", "安卓系统"),
        WINDOWS("Windows", "微软Windows系统"),
        MACOS("macOS", "苹果macOS系统"),
        LINUX("Linux", "Linux系统"),
        OTHER("Other", "其他系统");

        private final String code;
        private final String description;

        Platform(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public static Platform fromCode(String code) {
            for (Platform platform : Platform.values()) {
                if (platform.code.equals(code)) {
                    return platform;
                }
            }
            throw new IllegalArgumentException("未知的平台类型: " + code);
        }
    }

    /**
     * 设备状态枚举
     */
    @Getter
    public enum Status {
        INACTIVE(0, "非活跃"),
        ACTIVE(1, "活跃");

        private final Integer code;
        private final String description;

        Status(Integer code, String description) {
            this.code = code;
            this.description = description;
        }

        public static Status fromCode(Integer code) {
            for (Status status : Status.values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("未知的设备状态: " + code);
        }
    }

    /**
     * 判断设备是否活跃
     *
     * @return true-活跃，false-非活跃
     */
    public boolean isDeviceActive() {
        return Status.ACTIVE.getCode().equals(this.isActive);
    }

    /**
     * 设置设备为活跃状态
     */
    public void setActiveStatus() {
        this.isActive = Status.ACTIVE.getCode();
    }

    /**
     * 设置设备为非活跃状态
     */
    public void setInactiveStatus() {
        this.isActive = Status.INACTIVE.getCode();
    }

    /**
     * 获取设备类型枚举
     *
     * @return 设备类型枚举
     */
    public DeviceType getDeviceTypeEnum() {
        return this.deviceType != null ? DeviceType.fromCode(this.deviceType) : null;
    }

    /**
     * 设置设备类型
     *
     * @param deviceType 设备类型枚举
     */
    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType != null ? deviceType.getCode() : null;
    }

    /**
     * 获取平台类型枚举
     *
     * @return 平台类型枚举
     */
    public Platform getPlatformEnum() {
        return this.platform != null ? Platform.fromCode(this.platform) : null;
    }

    /**
     * 设置平台类型
     *
     * @param platform 平台类型枚举
     */
    public void setPlatform(Platform platform) {
        this.platform = platform != null ? platform.getCode() : null;
    }

    /**
     * 获取设备状态枚举
     *
     * @return 设备状态枚举
     */
    public Status getStatusEnum() {
        return this.isActive != null ? Status.fromCode(this.isActive) : null;
    }

    /**
     * 设置设备状态
     *
     * @param status 设备状态枚举
     */
    public void setStatus(Status status) {
        this.isActive = status != null ? status.getCode() : null;
    }
}