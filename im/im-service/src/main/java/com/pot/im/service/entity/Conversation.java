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
@TableName("im_conversation")
public class Conversation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

        @TableId(value = "id", type = IdType.AUTO)
    private Long id;

        @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

        @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

        @TableField("conversation_id")
    private Long conversationId;

        @TableField("type")
    private Integer type;

        @TableField("target_id")
    private Long targetId;

        @TableField("last_message_id")
    private Long lastMessageId;

        @TableField("last_message_time")
    private LocalDateTime lastMessageTime;

        @TableField("last_message_content")
    private String lastMessageContent;

        @TableField("message_count")
    private Long messageCount;

        @TableField("status")
    private Integer status;

        @TableField("extend_json")
    private String extendJson;
}
