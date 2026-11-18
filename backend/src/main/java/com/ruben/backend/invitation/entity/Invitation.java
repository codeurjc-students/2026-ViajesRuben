package com.ruben.backend.invitation.entity;

import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * An invitation to join a trip, addressed to an email. The email may have no account yet: the
 * invited person registers and then answers (I2). The token is what the link in the email carries;
 * it is never exposed by the API.
 */
@Entity
@Table(
    indexes =
        @Index(
            name = "idx_invitation_trip_email_status",
            columnList = "trip_id, invited_email, status"))
public class Invitation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Lazy, so reading the trip's id never loads the trip. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "trip_id", nullable = false)
  private Trip trip;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "inviter_id", nullable = false)
  private User inviter;

  /** Normalised like the email of an account: trimmed and lower case. */
  @Column(nullable = false, length = 255)
  private String invitedEmail;

  /** The account that owns the email when the invitation was made; empty when there was none. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invited_user_id")
  private User invitedUser;

  @Column(nullable = false, unique = true, length = 64)
  private String token;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InvitationStatus status;

  @Column(nullable = false)
  private Instant sentAt;

  protected Invitation() {}

  public Invitation(Trip trip, User inviter, String invitedEmail, User invitedUser, String token) {
    this.trip = trip;
    this.inviter = inviter;
    this.invitedEmail = invitedEmail;
    this.invitedUser = invitedUser;
    this.token = token;
    this.status = InvitationStatus.PENDING;
    // The database keeps microseconds: cut here, so the answer to the creation matches what is
    // read.
    this.sentAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  public Long getId() {
    return id;
  }

  public Trip getTrip() {
    return trip;
  }

  public User getInviter() {
    return inviter;
  }

  public String getInvitedEmail() {
    return invitedEmail;
  }

  public User getInvitedUser() {
    return invitedUser;
  }

  public String getToken() {
    return token;
  }

  public InvitationStatus getStatus() {
    return status;
  }

  public Instant getSentAt() {
    return sentAt;
  }
}
