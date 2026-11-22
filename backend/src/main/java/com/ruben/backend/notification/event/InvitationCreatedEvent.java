package com.ruben.backend.notification.event;

/**
 * Published when an invitation was created, so that the notification code sends its email once the
 * transaction has committed. It carries plain data only, no entities: it is the whole contract with
 * the notification package, which could become its own service later.
 *
 * <p>The token and the email must not end up in a log, so {@link #toString()} shows only the id.
 */
public record InvitationCreatedEvent(
    Long invitationId, String invitedEmail, String token, String inviterName, String tripName) {

  @Override
  public String toString() {
    return "InvitationCreatedEvent[invitationId=" + invitationId + "]";
  }
}
