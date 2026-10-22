package com.ruben.backend.image.service;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/** Limits for uploaded images (`app.images.*`). */
@Validated
@ConfigurationProperties("app.images")
public record ImageProperties(@NotNull DataSize maxSize) {}
