package com.pot.member.facade.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record CreateMemberRequest(
                @NotBlank(message = "Nickname must not be blank") String nickname,
                @NotBlank(message = "Password must not be blank") String password,
                @Email(message = "Invalid email address") String email,
                @Pattern(regexp = "^(1[3-9]\\d{9})?$", message = "Invalid phone number format") String phone) {
}
