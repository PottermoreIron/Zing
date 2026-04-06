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
@TableName("im_friend_request")
public class FriendRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

        @TableId(value = "id", type = IdType.AUTO)
    private Long id;

        @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

        @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

        @TableField("requester_id")
    private Long requesterId;

        @TableField("receiver_id")
    private Long receiverId;

        @TableField("message")
    private String message;

        @TableField("status")
    private Integer status;

        @TableField("source")
    private String source;

        @TableField("handled_at")
    private LocalDateTime handledAt;

        @TableField("expire_at")
    private LocalDateTime expireAt;

        @TableField("extend_json")
    private String extendJson;
}
