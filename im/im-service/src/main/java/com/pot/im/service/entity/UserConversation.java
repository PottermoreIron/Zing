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
@TableName("im_user_conversation")
public class UserConversation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

        @TableId(value = "id", type = IdType.AUTO)
    private Long id;

        @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

        @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

        @TableField("user_id")
    private Long userId;

        @TableField("conversation_id")
    private Long conversationId;

        @TableField("last_read_message_id")
    private Long lastReadMessageId;

        @TableField("last_read_time")
    private LocalDateTime lastReadTime;

        @TableField("unread_count")
    private Integer unreadCount;

        @TableField("is_muted")
    private Integer isMuted;

        @TableField("is_pinned")
    private Integer isPinned;

        @TableField("is_hidden")
    private Integer isHidden;

        @TableField("pinned_at")
    private LocalDateTime pinnedAt;

        @TableField("deleted_at")
    private LocalDateTime deletedAt;

        @TableField("extend_json")
    private String extendJson;
}
