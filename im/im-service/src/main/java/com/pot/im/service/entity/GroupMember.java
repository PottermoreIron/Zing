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
@TableName("im_group_member")
public class GroupMember implements Serializable {

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

        @TableField("user_id")
    private Long userId;

        @TableField("role")
    private Integer role;

        @TableField("nickname")
    private String nickname;

        @TableField("join_time")
    private LocalDateTime joinTime;

        @TableField("mute_until")
    private LocalDateTime muteUntil;

        @TableField("last_read_message_id")
    private String lastReadMessageId;

        @TableField("last_read_time")
    private LocalDateTime lastReadTime;

        @TableField("status")
    private Integer status;

        @TableField("extend_json")
    private String extendJson;
}
