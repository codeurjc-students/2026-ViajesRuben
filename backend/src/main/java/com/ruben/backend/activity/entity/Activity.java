package com.ruben.backend.activity.entity;

import com.ruben.backend.image.entity.Image;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** One item of a trip's itinerary. Its duration is not stored: it is end time minus start time. */
@Entity
@Table(
    indexes =
        @Index(name = "idx_activity_trip_date_start", columnList = "trip_id, date, start_time"))
public class Activity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Lazy, so reading the trip's id never loads the trip. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "trip_id", nullable = false)
  private Trip trip;

  /** Who created it; the creator of the trip for now, any participant once I4 exists. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 2000)
  private String description;

  @Column(length = 200)
  private String location;

  @Column(nullable = false)
  private LocalDate date;

  /** Both times are optional: an activity without them lasts the whole day. */
  private LocalTime startTime;

  private LocalTime endTime;

  /** Optional: an activity without cost adds nothing to the budget. */
  @Column(precision = 10, scale = 2)
  private BigDecimal cost;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private ActivityCategory category;

  /** Lazy, so loading an activity never loads the bytes of the picture. */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "image_id", unique = true)
  private Image image;

  @Column(nullable = false)
  private Instant createdAt;

  protected Activity() {}

  public Activity(Trip trip, User author, String name, LocalDate date) {
    this.trip = trip;
    this.author = author;
    this.name = name;
    this.date = date;
    this.createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public Trip getTrip() {
    return trip;
  }

  public User getAuthor() {
    return author;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  public BigDecimal getCost() {
    return cost;
  }

  public void setCost(BigDecimal cost) {
    this.cost = cost;
  }

  public ActivityCategory getCategory() {
    return category;
  }

  public void setCategory(ActivityCategory category) {
    this.category = category;
  }

  public Image getImage() {
    return image;
  }

  public void setImage(Image image) {
    this.image = image;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
