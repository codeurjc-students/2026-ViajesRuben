package com.ruben.backend.image.dto;

import com.ruben.backend.image.entity.Image;
import java.util.Arrays;
import java.util.Objects;

/** The bytes of an image and the type to answer them with. */
public record ImageContentDTO(byte[] data, String contentType) {

  public static ImageContentDTO from(Image image) {
    return new ImageContentDTO(image.getData(), image.getContentType());
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ImageContentDTO that
        && Arrays.equals(data, that.data)
        && Objects.equals(contentType, that.contentType);
  }

  @Override
  public int hashCode() {
    return 31 * Arrays.hashCode(data) + Objects.hashCode(contentType);
  }

  /** Prints the size instead of the bytes, so logs never dump an image. */
  @Override
  public String toString() {
    int size = data == null ? 0 : data.length;
    return "ImageContentDTO[data=" + size + " bytes, contentType=" + contentType + "]";
  }
}
