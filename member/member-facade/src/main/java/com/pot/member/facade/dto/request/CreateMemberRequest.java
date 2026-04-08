package com.pot.member.facade.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record CreateMemberRequest(
                @NotBlank(message = "昵称不能为空") String nickname,
                @NotBlank(message = "密码不能为空") String password,
                @Email(message = "邮箱格式不正确") String email,
                @Pattern(regexp = "^(1[3-9]\\d{9})?$", message = "手机号格式不正确") String phone) {
}
