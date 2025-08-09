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
 * 会话表
 * </p>
 *
 * @author Pot
 * @since 2025-08-10 01:03:52
 */
@Getter
@Setter
@ToString
@TableName("im_conversation")
public class Conversation implements Serializable {

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
     * 会话唯一标识
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 类型: 1-单聊, 2-群聊, 3-系统消息
     */
    @TableField("type")
    private Integer type;

    /**
     * 目标ID（好友ID或群组ID）
     */
    @TableField("target_id")
    private Long targetId;

    /**
     * 最后一条消息ID
     */
    @TableField("last_message_id")
    private Long lastMessageId;

    /**
     * 最后消息时间
     */
    @TableField("last_message_time")
    private LocalDateTime lastMessageTime;

    /**
     * 最后消息内容摘要
     */
    @TableField("last_message_content")
    private String lastMessageContent;

    /**
     * 消息总数
     */
    @TableField("message_count")
    private Long messageCount;

    /**
     * 状态: 0-已删除, 1-正常
     */
    @TableField("status")
    private Integer status;

    /**
     * 业务扩展json
     */
    @TableField("extend_json")
    private String extendJson;
}
