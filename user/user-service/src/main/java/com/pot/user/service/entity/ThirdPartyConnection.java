package com.pot.user.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
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

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "数据库默认id")
    private Long id;

    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @TableField("gmt_updated")
    @Schema(description = "修改时间")
    private LocalDateTime gmtUpdated;

    @TableField("connection_id")
    @Schema(description = "业务id")
    @NotNull(message = "业务id不能为空")
    private Long connectionId;

    @TableField("uid")
    @Schema(description = "用户id")
    @NotNull(message = "用户id不能为空")
    private Long uid;

    @TableField("platform_type")
    @Schema(description = "三方平台类型")
    @NotBlank(message = "三方平台类型不能为空")
    private String platformType;

    @TableField("third_party_user_id")
    @Schema(description = "三方平台userId")
    @NotBlank(message = "三方平台userId不能为空")
    private String thirdPartyUserId;

    @TableField("access_token")
    @Schema(description = "三方平台access_token")
    @NotBlank(message = "access_token不能为空")
    private String accessToken;

    @TableField("refresh_token")
    @Schema(description = "三方平台refresh_token")
    private String refreshToken;

    @TableField("expires_at")
    @Schema(description = "access_token过期时间")
    private LocalDateTime expiresAt;

    @TableLogic
    @TableField("is_deleted")
    @Schema(description = "删除标识位(0代表没删除，1代表删除)")
    private Integer deleted;
}
