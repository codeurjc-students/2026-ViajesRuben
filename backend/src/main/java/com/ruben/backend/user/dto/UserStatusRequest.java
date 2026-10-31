package com.ruben.backend.user.dto;

import com.ruben.backend.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

/** Blocks (`BLOCKED`) or unblocks (`ACTIVE`) an account. */
public record UserStatusRequest(@NotNull UserStatus status) {}
