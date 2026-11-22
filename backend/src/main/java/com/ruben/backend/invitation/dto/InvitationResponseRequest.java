package com.ruben.backend.invitation.dto;

import com.ruben.backend.invitation.entity.InvitationStatus;
import jakarta.validation.constraints.NotNull;

/** The answer of the invited person: `ACCEPTED` or `REJECTED`; `PENDING` is refused. */
public record InvitationResponseRequest(@NotNull InvitationStatus status) {}
