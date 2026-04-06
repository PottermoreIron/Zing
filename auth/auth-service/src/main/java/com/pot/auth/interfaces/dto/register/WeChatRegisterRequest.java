package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Register request used by legacy WeChat flows.
 */
public record WeChatRegisterRequest(
        @NotNull(message = "注册类型不能为空") @JsonProperty("registerType") RegisterType registerType,

        @NotBlank(message = "微信授权码不能为空") String code,

        String state,

        @JsonProperty("userDomain") UserDomain userDomain) implements RegisterRequest {
}
