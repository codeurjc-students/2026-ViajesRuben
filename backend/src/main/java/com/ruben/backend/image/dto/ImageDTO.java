package com.ruben.backend.image.dto;

import com.ruben.backend.image.entity.Image;
import java.util.UUID;

/** What the API says about a stored image; the bytes are served by `GET /api/v1/images/{id}`. */
public record ImageDTO(UUID id, String contentType, long size) {

  public static ImageDTO from(Image image) {
    return new ImageDTO(image.getId(), image.getContentType(), image.getData().length);
  }
}
