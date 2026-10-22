package com.ruben.backend.common.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** The single administrator, defined in configuration with a BCrypt-hashed password. */
@Validated
@ConfigurationProperties("app.admin")
public record AdminProperties(
    @NotBlank @Email String email,
    @NotBlank String name,
    @NotBlank @Pattern(regexp = "^\\$2[aby]\\$\\d{2}\\$.{53}$", message = "must be a BCrypt hash")
        String passwordHash) {}
