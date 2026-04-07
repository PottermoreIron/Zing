package com.pot.member.facade.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateMemberRequest {
        @NotBlank(message = "昵称不能为空")
        private String nickname;

        @NotBlank(message = "密码不能为空")
        private String password;

        @Email(message = "邮箱格式不正确")
        private String email;

        @Pattern(regexp = "^(1[3-9]\\d{9})?$", message = "手机号格式不正确")
        private String phone;
}
