package com.pot.auth.service.dto.wechat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 微信扫码登录二维码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "微信扫码登录二维码响应")
public class WeChatQrCodeResponse {

    @Schema(description = "二维码URL，前端需要生成二维码图片", example = "https://open.weixin.qq.com/connect/qrconnect?appid=xxx...")
    private String qrCodeUrl;

    @Schema(description = "State参数，用于防CSRF攻击", example = "random_state_string")
    private String state;

    @Schema(description = "二维码过期时间（秒）", example = "300")
    private Integer expireSeconds;

    @Schema(description = "二维码唯一标识（可选）", example = "qr_123456")
    private String qrCodeId;
}