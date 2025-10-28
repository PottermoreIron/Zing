package com.pot.auth.service.dto.request.login;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/22
 * @description: OAuth2登录请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "OAuth2登录请求")
public class OAuth2LoginRequest extends LoginRequest {

    @NotBlank(message = "授权码不能为空")
    @Schema(description = "OAuth2授权码", example = "4/0AY0e-g7...")
    private String code;

    @NotBlank(message = "state参数不能为空")
    @Schema(description = "防CSRF攻击的state参数", example = "random_state_string")
    private String state;

    @Schema(description = "OAuth2提供商", example = "github")
    private String provider;
}
