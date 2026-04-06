package com.pot.member.facade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindSocialAccountRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

        @NotNull(message = "用户ID不能为空")
    private Long memberId;

        @NotBlank(message = "平台提供商不能为空")
    private String provider;

        @NotBlank(message = "第三方平台用户ID不能为空")
    private String providerMemberId;

        private String providerUsername;

        private String providerEmail;

        @NotBlank(message = "访问令牌不能为空")
    private String accessToken;

        private String refreshToken;

        private Long tokenExpiresAt;

        private String scope;

        private String extendJson;
}


