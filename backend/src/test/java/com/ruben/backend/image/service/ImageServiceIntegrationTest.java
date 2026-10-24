package com.ruben.backend.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ImageServiceIntegrationTest {

  @Autowired private ImageService imageService;
  @Autowired private ImageRepository imageRepository;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    // Users first: another test may have left a user whose avatar is one of these images.
    userRepository.deleteAll();
    imageRepository.deleteAll();
  }

  @Test
  void storesAnImageInTheDatabaseAndReadsTheSameBytesBack() {
    byte[] data = ImageTestData.png(1024 * 1024);

    ImageDTO stored = imageService.store(new MockMultipartFile("file", "a.png", "image/png", data));

    assertThat(stored.id()).isNotNull();
    assertThat(stored.size()).isEqualTo(data.length);
    assertThat(imageRepository.count()).isEqualTo(1);
    ImageContentDTO content = imageService.findContent(stored.id());
    assertThat(content.contentType()).isEqualTo("image/png");
    assertThat(content.data()).isEqualTo(data);
  }

  @Test
  void eachImageGetsItsOwnId() {
    ImageDTO first =
        imageService.store(
            new MockMultipartFile("file", "a.jpg", "image/jpeg", ImageTestData.jpeg(100)));
    ImageDTO second =
        imageService.store(
            new MockMultipartFile("file", "b.webp", "image/webp", ImageTestData.webp(100)));

    assertThat(first.id()).isNotEqualTo(second.id());
    assertThat(imageService.findContent(second.id()).contentType()).isEqualTo("image/webp");
  }

  @Test
  void anInvalidImageIsNotStored() {
    assertThatThrownBy(
            () ->
                imageService.store(
                    new MockMultipartFile("file", "a.png", "image/png", ImageTestData.text())))
        .isInstanceOf(BadRequestException.class);

    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void anUnknownIdIsNotFound() {
    assertThatThrownBy(() -> imageService.findContent(UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
