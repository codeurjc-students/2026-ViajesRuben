package com.ruben.backend.invitation.entity;

/** Where an invitation stands: waiting for an answer, or answered by the invited person (I2). */
public enum InvitationStatus {
  PENDING,
  ACCEPTED,
  REJECTED
}
