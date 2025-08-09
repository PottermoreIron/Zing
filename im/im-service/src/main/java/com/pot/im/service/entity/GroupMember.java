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
 * 群组成员表
 * </p>
 *
 * @author Pot
 * @since 2025-08-10 01:03:52
 */
@Getter
@Setter
@ToString
@TableName("im_group_member")
public class GroupMember implements Serializable {

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
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 角色: 1-普通成员, 2-管理员, 3-群主
     */
    @TableField("role")
    private Integer role;

    /**
     * 群内昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 加入时间
     */
    @TableField("join_time")
    private LocalDateTime joinTime;

    /**
     * 禁言到期时间
     */
    @TableField("mute_until")
    private LocalDateTime muteUntil;

    /**
     * 最后已读消息ID
     */
    @TableField("last_read_message_id")
    private String lastReadMessageId;

    /**
     * 最后已读时间
     */
    @TableField("last_read_time")
    private LocalDateTime lastReadTime;

    /**
     * 状态: 0-已退出, 1-正常, 2-被踢出
     */
    @TableField("status")
    private Integer status;

    /**
     * 业务扩展json
     */
    @TableField("extend_json")
    private String extendJson;
}
