package com.ruben.backend.image.service;

import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.PayloadTooLargeException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.image.repository.ImageRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {

  private static final String JPEG = "image/jpeg";
  private static final String PNG = "image/png";
  private static final String WEBP = "image/webp";

  private final ImageRepository imageRepository;
  private final ImageProperties properties;

  public ImageService(ImageRepository imageRepository, ImageProperties properties) {
    this.imageRepository = imageRepository;
    this.properties = properties;
  }

  /**
   * Stores a JPEG, PNG or WebP image. The declared content type is not trusted: the first bytes of
   * the file must match it.
   */
  public ImageDTO store(MultipartFile file) {
    if (file.isEmpty()) {
      throw new BadRequestException("The image is empty");
    }
    if (file.getSize() > properties.maxSize().toBytes()) {
      throw new PayloadTooLargeException(
          "The image is larger than " + properties.maxSize().toMegabytes() + " MB");
    }
    byte[] data = read(file);
    String contentType = file.getContentType();
    if (contentType == null || !contentType.equals(detectType(data))) {
      throw new BadRequestException("Only JPEG, PNG and WebP images are accepted");
    }
    return ImageDTO.from(imageRepository.save(new Image(data, contentType)));
  }

  public ImageContentDTO findContent(UUID id) {
    return imageRepository
        .findById(id)
        .map(ImageContentDTO::from)
        .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
  }

  private static byte[] read(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Recognises the type from the file signature, or returns null when it is none of the three. */
  private static String detectType(byte[] data) {
    if (startsWith(data, 0, 0xFF, 0xD8, 0xFF)) {
      return JPEG;
    }
    if (startsWith(data, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
      return PNG;
    }
    // WebP is a RIFF container: "RIFF", a 4-byte size, then "WEBP".
    if (startsWith(data, 0, 'R', 'I', 'F', 'F') && startsWith(data, 8, 'W', 'E', 'B', 'P')) {
      return WEBP;
    }
    return null;
  }

  private static boolean startsWith(byte[] data, int offset, int... expected) {
    if (data.length < offset + expected.length) {
      return false;
    }
    for (int i = 0; i < expected.length; i++) {
      if ((data[offset + i] & 0xFF) != expected[i]) {
        return false;
      }
    }
    return true;
  }
}
