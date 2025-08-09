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
 * 用户会话关系表
 * </p>
 *
 * @author Pot
 * @since 2025-08-10 01:03:52
 */
@Getter
@Setter
@ToString
@TableName("im_user_conversation")
public class UserConversation implements Serializable {

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
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会话ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 最后已读消息ID
     */
    @TableField("last_read_message_id")
    private Long lastReadMessageId;

    /**
     * 最后已读时间
     */
    @TableField("last_read_time")
    private LocalDateTime lastReadTime;

    /**
     * 未读数量
     */
    @TableField("unread_count")
    private Integer unreadCount;

    /**
     * 是否免打扰: 0-否, 1-是
     */
    @TableField("is_muted")
    private Integer isMuted;

    /**
     * 是否置顶: 0-否, 1-是
     */
    @TableField("is_pinned")
    private Integer isPinned;

    /**
     * 是否隐藏: 0-否, 1-是
     */
    @TableField("is_hidden")
    private Integer isHidden;

    /**
     * 置顶时间
     */
    @TableField("pinned_at")
    private LocalDateTime pinnedAt;

    /**
     * 删除时间，NULL表示未删除
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 业务扩展json
     */
    @TableField("extend_json")
    private String extendJson;
}
