package com.pot.im.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@TableName("im_group")
public class Group implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

        @TableId(value = "id", type = IdType.AUTO)
    private Long id;

        @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

        @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

        @TableField("group_id")
    private Long groupId;

        @TableField("name")
    private String name;

        @TableField("description")
    private String description;

        @TableField("avatar")
    private String avatar;

        @TableField("owner_id")
    private Long ownerId;

        @TableField("type")
    private Integer type;

        @TableField("max_members")
    private Integer maxMembers;

        @TableField("current_members")
    private Integer currentMembers;

        @TableField("join_type")
    private Integer joinType;

        @TableField("mute_all")
    private Integer muteAll;

        @TableField("allow_member_invite")
    private Integer allowMemberInvite;

        @TableField("show_member_list")
    private Integer showMemberList;

        @TableField("status")
    private Integer status;

        @TableField("last_activity_time")
    private LocalDateTime lastActivityTime;

        @TableField("extend_json")
    private String extendJson;
}
