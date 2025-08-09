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
 * 好友申请表
 * </p>
 *
 * @author Pot
 * @since 2025-08-10 01:03:52
 */
@Getter
@Setter
@ToString
@TableName("im_friend_request")
public class FriendRequest implements Serializable {

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
     * 发起申请的用户ID
     */
    @TableField("requester_id")
    private Long requesterId;

    /**
     * 接收申请的用户ID
     */
    @TableField("receiver_id")
    private Long receiverId;

    /**
     * 申请消息
     */
    @TableField("message")
    private String message;

    /**
     * 状态: 0-待处理, 1-已同意, 2-已拒绝, 3-已过期
     */
    @TableField("status")
    private Integer status;

    /**
     * 添加来源（如手机号、二维码、推荐）
     */
    @TableField("source")
    private String source;

    /**
     * 处理时间
     */
    @TableField("handled_at")
    private LocalDateTime handledAt;

    /**
     * 过期时间
     */
    @TableField("expire_at")
    private LocalDateTime expireAt;

    /**
     * 业务扩展json
     */
    @TableField("extend_json")
    private String extendJson;
}
