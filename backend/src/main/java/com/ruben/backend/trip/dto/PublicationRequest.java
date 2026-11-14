package com.ruben.backend.trip.dto;

import jakarta.validation.constraints.NotNull;

/** Publishes (`true`) or unpublishes (`false`) a trip. */
public record PublicationRequest(@NotNull Boolean published) {}
