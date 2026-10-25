package com.ruben.backend.trip.entity;

import com.ruben.backend.image.entity.Image;
import com.ruben.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
public class Trip {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Lazy, so reading the creator's id never loads the user. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "creator_id", nullable = false)
  private User creator;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 100)
  private String destination;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column(nullable = false)
  private LocalDate endDate;

  /** Optional: a trip without budget has no budget conflicts. */
  @Column(precision = 10, scale = 2)
  private BigDecimal budget;

  @Column(length = 2000)
  private String description;

  /** Lazy, so loading a trip never loads the bytes of the picture. */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cover_image_id", unique = true)
  private Image coverImage;

  @Column(nullable = false)
  private Instant createdAt;

  protected Trip() {}

  public Trip(
      User creator, String name, String destination, LocalDate startDate, LocalDate endDate) {
    this.creator = creator;
    this.name = name;
    this.destination = destination;
    this.startDate = startDate;
    this.endDate = endDate;
    this.createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public User getCreator() {
    return creator;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public BigDecimal getBudget() {
    return budget;
  }

  public void setBudget(BigDecimal budget) {
    this.budget = budget;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Image getCoverImage() {
    return coverImage;
  }

  public void setCoverImage(Image coverImage) {
    this.coverImage = coverImage;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
