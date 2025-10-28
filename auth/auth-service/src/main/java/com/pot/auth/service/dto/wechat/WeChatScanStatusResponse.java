package com.pot.auth.service.dto.wechat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 微信扫码状态响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "微信扫码状态响应")
public class WeChatScanStatusResponse {

    @Schema(description = "扫码状态：PENDING-待扫码, SCANNED-已扫码, CONFIRMED-已确认, EXPIRED-已过期, CANCELLED-已取消",
            example = "PENDING")
    private String status;
    @Schema(description = "状态描述信息", example = "等待用户扫码")
    private String message;
    @Schema(description = "授权码（仅在CONFIRMED状态时返回）", example = "code_xxx")
    private String code;
    @Schema(description = "用户信息（仅在CONFIRMED状态时返回）")
    private Object userInfo;

    /**
     * 创建已过期响应
     */
    public static WeChatScanStatusResponse expired() {
        return WeChatScanStatusResponse.builder()
                .status(ScanStatus.EXPIRED.name())
                .message("二维码已过期，请重新获取")
                .build();
    }

    /**
     * 创建待扫码响应
     */
    public static WeChatScanStatusResponse pending() {
        return WeChatScanStatusResponse.builder()
                .status(ScanStatus.PENDING.name())
                .message("等待用户扫码")
                .build();
    }

    /**
     * 创建已扫码响应
     */
    public static WeChatScanStatusResponse scanned() {
        return WeChatScanStatusResponse.builder()
                .status(ScanStatus.SCANNED.name())
                .message("用户已扫码，等待确认")
                .build();
    }

    /**
     * 创建已确认响应
     */
    public static WeChatScanStatusResponse confirmed(String code) {
        return WeChatScanStatusResponse.builder()
                .status(ScanStatus.CONFIRMED.name())
                .message("用户已确认授权")
                .code(code)
                .build();
    }

    /**
     * 扫码状态枚举
     */
    public enum ScanStatus {
        PENDING("待扫码"),
        SCANNED("已扫码"),
        CONFIRMED("已确认"),
        EXPIRED("已过期"),
        CANCELLED("已取消");

        private final String description;

        ScanStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}

