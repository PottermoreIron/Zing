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
@TableName("im_message")
public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

        @TableId(value = "id", type = IdType.AUTO)
    private Long id;

        @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

        @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

        @TableField("message_id")
    private Long messageId;

        @TableField("conversation_id")
    private Long conversationId;

        @TableField("sender_id")
    private Long senderId;

        @TableField("message_type")
    private Integer messageType;

        @TableField("content")
    private String content;

        @TableField("extra_data")
    private String extraData;

        @TableField("reply_to_message_id")
    private Long replyToMessageId;

        @TableField("forward_from_message_id")
    private Long forwardFromMessageId;

        @TableField("at_users")
    private String atUsers;

        @TableField("status")
    private Integer status;

        @TableField("read_count")
    private Integer readCount;

        @TableField("extend_json")
    private String extendJson;
}
