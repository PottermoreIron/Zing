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
@TableName("im_friend")
public class Friend implements Serializable {

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

        @TableField("friend_id")
    private Long friendId;

        @TableField("status")
    private Integer status;

        @TableField("remark")
    private String remark;

        @TableField("tags")
    private String tags;

        @TableField("source")
    private String source;

        @TableField("extend_json")
    private String extendJson;
}
