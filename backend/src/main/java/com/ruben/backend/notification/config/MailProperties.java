package com.ruben.backend.notification.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * What the emails need besides the SMTP connection (`spring.mail.*`): the sender and the public
 * address of the application, which the links in the emails start with (no trailing slash needed).
 */
@Validated
@ConfigurationProperties("app.mail")
public record MailProperties(@NotBlank String from, @NotBlank String publicUrl) {}
