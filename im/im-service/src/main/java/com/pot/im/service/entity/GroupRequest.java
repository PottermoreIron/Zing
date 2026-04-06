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
@TableName("im_group_request")
public class GroupRequest implements Serializable {

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

        @TableField("inviter_id")
    private Long inviterId;

        @TableField("message")
    private String message;

        @TableField("type")
    private Integer type;

        @TableField("status")
    private Integer status;

        @TableField("handled_by")
    private Long handledBy;

        @TableField("handled_at")
    private LocalDateTime handledAt;

        @TableField("expire_at")
    private LocalDateTime expireAt;

        @TableField("extend_json")
    private String extendJson;
}
