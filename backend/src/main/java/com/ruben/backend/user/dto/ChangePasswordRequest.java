package com.ruben.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The new password follows the registration rule; the current one proves who is asking. */
public record ChangePasswordRequest(
    @NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
