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
 * 用户好友关系表
 * </p>
 *
 * @author Pot
 * @since 2025-08-10 01:03:52
 */
@Getter
@Setter
@ToString
@TableName("im_friend")
public class Friend implements Serializable {

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
     * 发起方用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 好友用户ID
     */
    @TableField("friend_id")
    private Long friendId;

    /**
     * 状态: 0-已删除, 1-正常, 2-已拉黑
     */
    @TableField("status")
    private Integer status;

    /**
     * 备注名
     */
    @TableField("remark")
    private String remark;

    /**
     * 标签，JSON格式
     */
    @TableField("tags")
    private String tags;

    /**
     * 添加来源（如手机号、二维码、推荐）
     */
    @TableField("source")
    private String source;

    /**
     * 业务扩展json
     */
    @TableField("extend_json")
    private String extendJson;
}
