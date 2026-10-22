package com.ruben.backend.image.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.service.ImageService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageControllerUnitTest {

  @Mock private ImageService imageService;
  @InjectMocks private ImageController controller;

  @Test
  void uploadAnswers201WithTheLocationOfTheNewImage() {
    UUID id = UUID.randomUUID();
    MockMultipartFile file =
        new MockMultipartFile("file", "a.png", "image/png", ImageTestData.png(20));
    ImageDTO dto = new ImageDTO(id, "image/png", 20);
    when(imageService.store(file)).thenReturn(dto);

    ResponseEntity<ImageDTO> response = controller.upload(file);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/images/" + id);
    assertThat(response.getBody()).isEqualTo(dto);
  }

  @Test
  void getImageAnswersTheBytesWithTheContentTypeAndCacheHeaders() {
    UUID id = UUID.randomUUID();
    byte[] data = ImageTestData.jpeg(30);
    when(imageService.findContent(id)).thenReturn(new ImageContentDTO(data, "image/jpeg"));

    ResponseEntity<byte[]> response = controller.getImage(id);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(data);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
    assertThat(response.getHeaders().getContentLength()).isEqualTo(30);
    assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
        .contains("max-age=31536000", "public", "immutable");
  }
}
