package com.pot.member.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author pot
 * @since 2025-02-25
 */
@Data
@Builder
@TableName("t_user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

    @TableField("uid")
    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long uid;

    @TableField("device_id")
    @Schema(description = "设备ID")
    private String deviceId;

    @TableField("name")
    @Schema(description = "姓名")
    @NotBlank(message = "姓名不能为空")
    private String name;

    @TableField("nickname")
    @Schema(description = "昵称")
    private String nickname;

    @TableField("password")
    @Schema(description = "加密加盐密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    @TableField("email")
    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;

    @TableField("phone")
    @Schema(description = "手机号码")
    private String phone;

    @TableField("sex")
    @Schema(description = "性别 [0代表男, 1代表女]")
    @Min(value = 0, message = "性别只能为0或1")
    @Max(value = 1, message = "性别只能为0或1")
    private Integer sex;

    @TableField("avatar")
    @Schema(description = "头像地址")
    private String avatar;

    @TableField("age")
    @Schema(description = "年龄")
    @Min(value = 0, message = "年龄不能为负数")
    private Integer age;

    @TableField("birthday")
    @Schema(description = "生日")
    private LocalDate birthday;

    @TableField("address")
    @Schema(description = "地址")
    private String address;

    @TableField("language")
    @Schema(description = "设备语言")
    private String language;

    @TableField("country")
    @Schema(description = "国家")
    private String country;

    @TableField("status")
    @Schema(description = "账户状态 [0代表停用，1代表启用]")
    @NotNull(message = "账户状态不能为空")
    @Min(value = 0, message = "账户状态只能为0或1")
    @Max(value = 1, message = "账户状态只能为0或1")
    private Integer status;

    @TableField("register_time")
    @Schema(description = "账户注册时间")
    private LocalDateTime registerTime;

    @TableField("last_login_ip")
    @Schema(description = "最后一次登录ip地址")
    private String lastLoginIp;

    @TableField("last_login_date")
    @Schema(description = "最后一次登录时间")
    private LocalDateTime lastLoginDate;

    @TableLogic
    @TableField("is_deleted")
    @Schema(description = "删除标识位 [0代表没删除，1代表删除]")
    private Boolean deleted;

    @TableField("creator")
    @Schema(description = "创建者")
    private String creator;

    @TableField("updater")
    @Schema(description = "修改者")
    private String updater;

    @TableField("extend_json")
    @Schema(description = "业务扩展json")
    private String extendJson;
}
