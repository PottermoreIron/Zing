package com.pot.member.service.application.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RegisterMemberCommand(
        @NotBlank(message = "Nickname must not be blank") @Size(min = 2, max = 50, message = "Nickname must be between 2 and 50 characters") @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9_-]+$", message = "Nickname may only contain CJK characters, letters, digits, underscores, and hyphens") String nickname,
        @NotBlank(message = "Email must not be blank") @Email(message = "Invalid email address") String email,
        @NotBlank(message = "Password must not be blank") @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters") String password,
        @Pattern(regexp = "^(1[3-9]\\d{9})?$", message = "Invalid phone number format") String phoneNumber) {
}
