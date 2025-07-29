package com.pot.user.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
@ApiModel(value = "User对象", description = "用户表")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据库默认Id
     */
    @ApiModelProperty("数据库默认Id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    @ApiModelProperty("修改时间")
    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    /**
     * 用户id
     */
    @TableField("uid")
    @ApiModelProperty("用户id")
    private Long uid;

    /**
     * 设备id
     */
    @TableField("device_id")
    @ApiModelProperty("设备id")
    private String deviceId;

    /**
     * 姓名
     */
    @TableField("name")
    @ApiModelProperty("姓名")
    private String name;

    /**
     * 昵称
     */
    @ApiModelProperty("昵称")
    @TableField("nickname")
    private String nickname;

    /**
     * 加密加盐密码
     */
    @TableField("password")
    @ApiModelProperty("加密加盐密码")
    private String password;

    /**
     * 邮箱
     */
    @TableField("email")
    @ApiModelProperty("邮箱")
    @Email
    private String email;

    /**
     * 手机号码
     */
    @TableField("phone")
    @ApiModelProperty("手机号码")
    private String phone;

    /**
     * 性别 [0代表男, 1代表女]
     */
    @TableField("sex")
    @ApiModelProperty("性别 [0代表男, 1代表女]")
    private Integer sex;

    /**
     * 头像地址
     */
    @TableField("avatar")
    @ApiModelProperty("头像地址")
    private String avatar;

    /**
     * 年龄
     */
    @TableField("age")
    @ApiModelProperty("年龄")
    private Integer age;

    /**
     * 生日
     */
    @ApiModelProperty("生日")
    @TableField("birthday")
    private LocalDate birthday;

    /**
     * 地址
     */
    @TableField("address")
    @ApiModelProperty("地址")
    private String address;

    /**
     * 设备语言
     */
    @TableField("language")
    @ApiModelProperty("设备语言")
    private String language;

    /**
     * 国家
     */
    @TableField("country")
    @ApiModelProperty("国家")
    private String country;

    /**
     * 账户状态 [0代表停用，1代表启用]
     */
    @TableField("status")
    @ApiModelProperty("账户状态 [0代表停用，1代表启用]")
    private Integer status;

    /**
     * 账户注册时间
     */
    @ApiModelProperty("账户注册时间")
    @TableField("register_time")
    private LocalDateTime registerTime;

    /**
     * 最后一次登录ip地址
     */
    @TableField("last_login_ip")
    @ApiModelProperty("最后一次登录ip地址")
    @NotBlank(message = "最后一次登录ip地址不能为空")
    private String lastLoginIp;

    /**
     * 最后一次登录时间
     */
    @ApiModelProperty("最后一次登录时间")
    @TableField("last_login_date")
    private LocalDateTime lastLoginDate;

    /**
     * 删除标识位 [0代表没删除，1代表删除]
     */
    @TableLogic
    @TableField("is_deleted")
    @ApiModelProperty("删除标识位 [0代表没删除，1代表删除]")
    private Boolean deleted;

    /**
     * 创建者
     */
    @TableField("creator")
    @ApiModelProperty("创建者")
    private String creator;

    /**
     * 修改者
     */
    @TableField("updater")
    @ApiModelProperty("修改者")
    private String updater;

    /**
     * 业务扩展json
     */
    @TableField("extend_json")
    @ApiModelProperty("业务扩展json")
    private String extendJson;
}
