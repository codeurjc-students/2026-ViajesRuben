package com.ruben.backend.image.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.time.Instant;
import java.util.UUID;

/**
 * An uploaded picture, stored in the database. Linking it to users, trips or activities is done by
 * their own features.
 */
@Entity
public class Image {

  /** A random id, so image URLs cannot be guessed or enumerated. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** MEDIUMBLOB (up to 16 MB), above the 10 MB multipart limit. */
  @Lob
  @Column(nullable = false, length = 16_777_215)
  private byte[] data;

  @Column(nullable = false)
  private String contentType;

  @Column(nullable = false)
  private Instant createdAt;

  protected Image() {}

  public Image(byte[] data, String contentType) {
    this.data = data;
    this.contentType = contentType;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public byte[] getData() {
    return data;
  }

  public String getContentType() {
    return contentType;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
