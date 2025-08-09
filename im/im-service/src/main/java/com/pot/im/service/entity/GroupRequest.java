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
 * 群组申请表
 * </p>
 *
 * @author Pot
 * @since 2025-08-10 01:03:52
 */
@Getter
@Setter
@ToString
@TableName("im_group_request")
public class GroupRequest implements Serializable {

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
     * 群组ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 申请用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 邀请人ID
     */
    @TableField("inviter_id")
    private Long inviterId;

    /**
     * 申请消息
     */
    @TableField("message")
    private String message;

    /**
     * 类型: 1-申请加入, 2-邀请加入
     */
    @TableField("type")
    private Integer type;

    /**
     * 状态: 0-待处理, 1-已同意, 2-已拒绝, 3-已过期
     */
    @TableField("status")
    private Integer status;

    /**
     * 处理人ID
     */
    @TableField("handled_by")
    private Long handledBy;

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
