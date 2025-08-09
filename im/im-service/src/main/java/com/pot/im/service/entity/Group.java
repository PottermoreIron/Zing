package com.pot.im.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 群组信息表
 * </p>
 *
 * @author Pot
 * @since 2025-08-10 01:03:52
 */
@Getter
@Setter
@ToString
@TableName("im_group")
public class Group implements Serializable {

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
    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    /**
     * 群唯一标识（可用于公开邀请链接）
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 群名称
     */
    @TableField("name")
    private String name;

    /**
     * 群描述
     */
    @TableField("description")
    private String description;

    /**
     * 群头像URL
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 群主用户ID
     */
    @TableField("owner_id")
    private Long ownerId;

    /**
     * 类型: 1-普通群, 2-超级群
     */
    @TableField("type")
    private Integer type;

    /**
     * 最大成员数
     */
    @TableField("max_members")
    private Integer maxMembers;

    /**
     * 当前成员数
     */
    @TableField("current_members")
    private Integer currentMembers;

    /**
     * 加入方式: 1-自由加入, 2-需审核, 3-禁止加入
     */
    @TableField("join_type")
    private Integer joinType;

    /**
     * 是否全员禁言: 0-否, 1-是
     */
    @TableField("mute_all")
    private Integer muteAll;

    /**
     * 是否允许成员邀请: 0-否, 1-是
     */
    @TableField("allow_member_invite")
    private Integer allowMemberInvite;

    /**
     * 是否显示成员列表: 0-否, 1-是
     */
    @TableField("show_member_list")
    private Integer showMemberList;

    /**
     * 状态: 0-已解散, 1-正常, 2-已封禁
     */
    @TableField("status")
    private Integer status;

    /**
     * 最后活跃时间
     */
    @TableField("last_activity_time")
    private LocalDateTime lastActivityTime;

    /**
     * 业务扩展json
     */
    @TableField("extend_json")
    private String extendJson;
}
