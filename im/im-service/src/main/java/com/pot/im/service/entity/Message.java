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
 * 消息表
 * </p>
 *
 * @author Pot
 * @since 2025-08-10 01:03:52
 */
@Getter
@Setter
@ToString
@TableName("im_message")
public class Message implements Serializable {

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
     * 消息唯一标识
     */
    @TableField("message_id")
    private Long messageId;

    /**
     * 会话ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 发送者用户ID
     */
    @TableField("sender_id")
    private Long senderId;

    /**
     * 消息类型: 1-文本, 2-图片, 3-语音, 4-视频, 5-文件, 6-位置, 7-名片, 8-系统消息
     */
    @TableField("message_type")
    private Integer messageType;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 扩展数据（如文件信息、位置信息等）
     */
    @TableField("extra_data")
    private String extraData;

    /**
     * 回复的消息ID
     */
    @TableField("reply_to_message_id")
    private Long replyToMessageId;

    /**
     * 转发的原消息ID
     */
    @TableField("forward_from_message_id")
    private Long forwardFromMessageId;

    /**
     * @的用户ID列表
     */
    @TableField("at_users")
    private String atUsers;

    /**
     * 状态: 0-已删除, 1-正常, 2-已撤回
     */
    @TableField("status")
    private Integer status;

    /**
     * 已读人数（用于群聊）
     */
    @TableField("read_count")
    private Integer readCount;

    /**
     * 业务扩展json
     */
    @TableField("extend_json")
    private String extendJson;
}
