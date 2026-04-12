package com.pot.member.service.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordCommand(
        Long memberId,
        @NotBlank(message = "Current password must not be blank") String oldPassword,
        @NotBlank(message = "New password must not be blank") @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters") String newPassword) {
}
