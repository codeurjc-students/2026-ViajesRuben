package com.ruben.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The editable details of one's own profile, with the same rules as at registration. */
public record UpdateProfileRequest(
    @NotBlank @Size(max = 100) String name, @NotBlank @Email @Size(max = 255) String email) {}
