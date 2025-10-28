package com.pot.auth.service.dto.v1.oAuth2;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author: Pot
 * @created: 2025/10/26 00:07
 * @description: OAuth提供商信息
 */
@Schema(description = "OAuth提供商信息")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OAuthProviderInfo {
    @Schema(description = "提供商ID", example = "github")
    private String id;

    @Schema(description = "提供商名称", example = "GitHub")
    private String name;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "图标URL", example = "https://example.com/icons/github.png")
    private String iconUrl;

    @Schema(description = "授权范围", example = "[\"user:email\"]")
    private List<String> scopes;

    @Schema(description = "提供商类型", example = "oauth2")
    private String type;

    @Schema(description = "排序顺序", example = "1")
    private Integer sortOrder;
}
