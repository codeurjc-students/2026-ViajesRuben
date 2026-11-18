package com.ruben.backend.invitation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Data to invite someone to a trip: only the email, with the same rules as registration. */
public record InvitationRequest(@NotBlank @Email @Size(max = 255) String email) {}
