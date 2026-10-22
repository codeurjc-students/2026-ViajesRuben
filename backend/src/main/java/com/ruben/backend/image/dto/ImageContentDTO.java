package com.ruben.backend.image.dto;

import com.ruben.backend.image.entity.Image;

/** The bytes of an image and the type to answer them with. */
public record ImageContentDTO(byte[] data, String contentType) {

  public static ImageContentDTO from(Image image) {
    return new ImageContentDTO(image.getData(), image.getContentType());
  }
}
