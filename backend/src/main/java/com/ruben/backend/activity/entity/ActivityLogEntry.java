package com.ruben.backend.activity.entity;

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
 * One line of a trip's change log (I4): who added, edited or deleted which activity, and when. The
 * names of the activity and of the user are copied, so the line still reads the same after the
 * activity is deleted (its reference becomes null) or the user changes their name. For an edit it
 * lists the names of the fields that changed, never their values.
 */
@Entity
@Table(
    name = "activity_log_entry",
    indexes = @Index(name = "idx_activity_log_trip_created", columnList = "trip_id, created_at"))
public class ActivityLogEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "trip_id", nullable = false)
  private Trip trip;

  /** Null once the activity is deleted. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "activity_id")
  private Activity activity;

  @Column(nullable = false, length = 100)
  private String activityName;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String userName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ActivityLogAction action;

  /** Comma-separated field names; only for an edit, and null when nothing but the save happened. */
  @Column(length = 200)
  private String changedFields;

  @Column(nullable = false)
  private Instant createdAt;

  protected ActivityLogEntry() {}

  public ActivityLogEntry(
      Trip trip,
      Activity activity,
      String activityName,
      User user,
      String userName,
      ActivityLogAction action,
      String changedFields) {
    this.trip = trip;
    this.activity = activity;
    this.activityName = activityName;
    this.user = user;
    this.userName = userName;
    this.action = action;
    this.changedFields = changedFields;
    // The database keeps microseconds: cut here, so what is returned matches what is read.
    this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  public Long getId() {
    return id;
  }

  public Trip getTrip() {
    return trip;
  }

  public Activity getActivity() {
    return activity;
  }

  public String getActivityName() {
    return activityName;
  }

  public User getUser() {
    return user;
  }

  public String getUserName() {
    return userName;
  }

  public ActivityLogAction getAction() {
    return action;
  }

  public String getChangedFields() {
    return changedFields;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
