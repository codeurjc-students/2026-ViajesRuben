package com.ruben.backend.image.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("B1")
class ImageContentDTOUnitTest {

  @Test
  void twoContentsWithTheSameBytesAndTypeAreEqual() {
    ImageContentDTO first = new ImageContentDTO(new byte[] {1, 2, 3}, "image/png");
    ImageContentDTO second = new ImageContentDTO(new byte[] {1, 2, 3}, "image/png");

    assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
  }

  @Test
  void differentBytesOrTypeMakeContentsDifferent() {
    ImageContentDTO content = new ImageContentDTO(new byte[] {1, 2, 3}, "image/png");

    assertThat(content)
        .isNotEqualTo(new ImageContentDTO(new byte[] {1, 2, 4}, "image/png"))
        .isNotEqualTo(new ImageContentDTO(new byte[] {1, 2, 3}, "image/jpeg"))
        .isNotEqualTo("not a dto")
        .isNotEqualTo(null);
  }

  @Test
  void toStringShowsTheSizeAndNotTheBytes() {
    ImageContentDTO content = new ImageContentDTO(new byte[] {1, 2, 3}, "image/png");

    assertThat(content).hasToString("ImageContentDTO[data=3 bytes, contentType=image/png]");
  }

  @Test
  void toStringAndHashCodeTolerateMissingData() {
    ImageContentDTO content = new ImageContentDTO(null, null);

    assertThat(content).hasToString("ImageContentDTO[data=0 bytes, contentType=null]");
    assertThat(content.hashCode()).isEqualTo(new ImageContentDTO(null, null).hashCode());
  }
}
