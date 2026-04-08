package com.pot.member.facade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

@Builder
public record BindSocialAccountRequest(
        @NotNull(message = "用户ID不能为空") Long memberId,
        @NotBlank(message = "平台提供商不能为空") String provider,
        @NotBlank(message = "第三方平台用户ID不能为空") String providerMemberId,
        String providerUsername,
        String providerEmail,
        @NotBlank(message = "访问令牌不能为空") String accessToken,
        String refreshToken,
        Long tokenExpiresAt,
        String scope,
        String extendJson) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
