package com.pot.user.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 第三方平台连接表
 * </p>
 *
 * @author pot
 * @since 2025-04-06
 */
@Data
@Builder
@TableName("t_third_party_connection")
public class ThirdPartyConnection implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据库默认id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    @TableField("gmt_updated")
    private LocalDateTime gmtUpdated;

    /**
     * 业务id
     */
    @TableField("connection_id")
    private Long connectionId;

    /**
     * 用户id
     */
    @TableField("uid")
    private Long uid;

    /**
     * 三方平台类型
     */
    @TableField("platform_type")
    private String platformType;

    /**
     * 三方平台userId
     */
    @TableField("third_party_user_id")
    private String thirdPartyUserId;

    /**
     * 三方平台access_token
     */
    @TableField("access_token")
    private String accessToken;

    /**
     * 三方平台refresh_token
     */
    @TableField("refresh_token")
    private String refreshToken;

    /**
     * access_token过期时间
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /**
     * 删除标识位(0代表没删除，1代表删除)
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
