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
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_device")
public class Device implements Serializable {

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

    @TableField("device_id")
    private String deviceId;

    @TableField("device_type")
    private String deviceType;

    @TableField("platform")
    private String platform;

    @TableField("browser")
    private String browser;

    @TableField("app_version")
    private String appVersion;

    @TableField("push_token")
    private String pushToken;

    @TableField("is_active")
    private Integer isActive;

    @TableField("gmt_last_used_at")
    private LocalDateTime gmtLastUsedAt;

    public boolean isDeviceActive() {
        return Status.ACTIVE.getCode().equals(this.isActive);
    }

    public void setActiveStatus() {
        this.isActive = Status.ACTIVE.getCode();
    }

    public void setInactiveStatus() {
        this.isActive = Status.INACTIVE.getCode();
    }

    public DeviceType getDeviceTypeEnum() {
        return this.deviceType != null ? DeviceType.fromCode(this.deviceType) : null;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType != null ? deviceType.getCode() : null;
    }

    public Platform getPlatformEnum() {
        return this.platform != null ? Platform.fromCode(this.platform) : null;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform != null ? platform.getCode() : null;
    }

    public Status getStatusEnum() {
        return this.isActive != null ? Status.fromCode(this.isActive) : null;
    }

    public void setStatus(Status status) {
        this.isActive = status != null ? status.getCode() : null;
    }

    @Getter
    public enum DeviceType {
        MOBILE("MOBILE", "Mobile"),
        TABLET("TABLET", "Tablet"),
        DESKTOP("DESKTOP", "Desktop"),
        TV("TV", "TV"),
        WATCH("WATCH", "Wearable"),
        OTHER("OTHER", "Other");

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
            throw new IllegalArgumentException("Unknown device type: " + code);
        }
    }

    @Getter
    public enum Platform {
        IOS("iOS", "Apple iOS"),
        ANDROID("Android", "Android"),
        WINDOWS("Windows", "Microsoft Windows"),
        MACOS("macOS", "Apple macOS"),
        LINUX("Linux", "Linux"),
        OTHER("Other", "Other");

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
            throw new IllegalArgumentException("Unknown platform type: " + code);
        }
    }

    @Getter
    public enum Status {
        INACTIVE(0, "Inactive"),
        ACTIVE(1, "Active");

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
            throw new IllegalArgumentException("Unknown device status: " + code);
        }
    }
}