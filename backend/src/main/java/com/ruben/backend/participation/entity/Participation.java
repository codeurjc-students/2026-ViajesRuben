package com.ruben.backend.participation.entity;

import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * A user who joined a trip by accepting an invitation (I2). The trip's creator has none: the
 * creator is identified by the trip itself. A user joins a trip once.
 */
@Entity
@Table(
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_participation_trip_user",
            columnNames = {"trip_id", "user_id"}))
public class Participation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Lazy, so reading the trip's id never loads the trip. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "trip_id", nullable = false)
  private Trip trip;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private Instant joinedAt;

  protected Participation() {}

  public Participation(Trip trip, User user) {
    this.trip = trip;
    this.user = user;
    // The database keeps microseconds: cut here, so what is returned matches what is read.
    this.joinedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  public Long getId() {
    return id;
  }

  public Trip getTrip() {
    return trip;
  }

  public User getUser() {
    return user;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }
}
