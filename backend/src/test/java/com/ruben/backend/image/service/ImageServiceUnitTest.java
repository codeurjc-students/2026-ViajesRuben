package com.ruben.backend.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.PayloadTooLargeException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.image.repository.ImageRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class ImageServiceUnitTest {

  @Mock private ImageRepository imageRepository;

  private ImageService imageService;

  @BeforeEach
  void setUp() {
    imageService = new ImageService(imageRepository, new ImageProperties(DataSize.ofKilobytes(10)));
  }

  private static MockMultipartFile file(String contentType, byte[] data) {
    return new MockMultipartFile("file", "picture", contentType, data);
  }

  private void stubSave() {
    when(imageRepository.save(any(Image.class))).thenAnswer(call -> call.getArgument(0));
  }

  @Test
  void storesAJpegImage() {
    stubSave();
    byte[] data = ImageTestData.jpeg(100);

    ImageDTO image = imageService.store(file("image/jpeg", data));

    ArgumentCaptor<Image> saved = ArgumentCaptor.forClass(Image.class);
    verify(imageRepository).save(saved.capture());
    assertThat(saved.getValue().getData()).isEqualTo(data);
    assertThat(saved.getValue().getContentType()).isEqualTo("image/jpeg");
    assertThat(saved.getValue().getCreatedAt()).isNotNull();
    assertThat(image.contentType()).isEqualTo("image/jpeg");
    assertThat(image.size()).isEqualTo(100);
  }

  @Test
  void storesAPngImage() {
    stubSave();

    ImageDTO image = imageService.store(file("image/png", ImageTestData.png(100)));

    assertThat(image.contentType()).isEqualTo("image/png");
  }

  @Test
  void storesAWebpImage() {
    stubSave();

    ImageDTO image = imageService.store(file("image/webp", ImageTestData.webp(100)));

    assertThat(image.contentType()).isEqualTo("image/webp");
  }

  @Test
  void rejectsAnEmptyFile() {
    assertThatThrownBy(() -> imageService.store(file("image/png", new byte[0])))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("The image is empty");
    verify(imageRepository, never()).save(any());
  }

  @Test
  void rejectsATypeThatIsNotAllowed() {
    assertThatThrownBy(() -> imageService.store(file("image/gif", ImageTestData.png(100))))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Only JPEG, PNG and WebP images are accepted");
    verify(imageRepository, never()).save(any());
  }

  @Test
  void rejectsAFileWithoutContentType() {
    assertThatThrownBy(() -> imageService.store(file(null, ImageTestData.png(100))))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void rejectsAFileWhoseBytesDoNotMatchTheDeclaredType() {
    assertThatThrownBy(() -> imageService.store(file("image/png", ImageTestData.text())))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> imageService.store(file("image/jpeg", ImageTestData.png(100))))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> imageService.store(file("image/webp", ImageTestData.png(100))))
        .isInstanceOf(BadRequestException.class);
    verify(imageRepository, never()).save(any());
  }

  @Test
  void rejectsAFileTooShortToHaveASignature() {
    assertThatThrownBy(() -> imageService.store(file("image/png", new byte[] {1, 2})))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void rejectsAnImageLargerThanTheLimit() {
    byte[] tooBig = ImageTestData.png(10 * 1024 + 1);

    assertThatThrownBy(() -> imageService.store(file("image/png", tooBig)))
        .isInstanceOf(PayloadTooLargeException.class)
        .hasMessage("The image is larger than 0 MB");
    verify(imageRepository, never()).save(any());
  }

  @Test
  void acceptsAnImageOfExactlyTheLimit() {
    stubSave();

    ImageDTO image = imageService.store(file("image/png", ImageTestData.png(10 * 1024)));

    assertThat(image.size()).isEqualTo(10 * 1024);
  }

  @Test
  void saveReturnsTheStoredEntityForOtherDomainsToLink() {
    stubSave();
    byte[] data = ImageTestData.png(100);

    Image image = imageService.save(file("image/png", data));

    assertThat(image.getData()).isEqualTo(data);
    assertThat(image.getContentType()).isEqualTo("image/png");
  }

  @Test
  void saveAppliesTheSameChecksAsStore() {
    assertThatThrownBy(() -> imageService.save(file("image/png", ImageTestData.text())))
        .isInstanceOf(BadRequestException.class);
    verify(imageRepository, never()).save(any());
  }

  @Test
  void deleteRemovesTheImageWithoutLoadingIt() {
    UUID id = UUID.randomUUID();

    imageService.delete(id);

    verify(imageRepository).deleteWithoutLoading(id);
    verify(imageRepository, never()).findById(any());
  }

  @Test
  void findContentReturnsTheBytesAndTheType() {
    UUID id = UUID.randomUUID();
    byte[] data = ImageTestData.png(50);
    when(imageRepository.findById(id)).thenReturn(Optional.of(new Image(data, "image/png")));

    ImageContentDTO content = imageService.findContent(id);

    assertThat(content.data()).isEqualTo(data);
    assertThat(content.contentType()).isEqualTo("image/png");
  }

  @Test
  void findContentThrowsWhenTheImageDoesNotExist() {
    UUID id = UUID.randomUUID();
    when(imageRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> imageService.findContent(id))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Image not found");
  }
}
