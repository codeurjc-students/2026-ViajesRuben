package com.ruben.backend.image.controller;

import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
public class ImageController {

  /** An image never changes (a new upload gets a new id), so browsers may cache it for a year. */
  private static final CacheControl CACHE =
      CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

  private final ImageService imageService;

  public ImageController(ImageService imageService) {
    this.imageService = imageService;
  }

  @Operation(
      summary = "Upload an image",
      description =
          "Multipart form with the picture in the `file` part: JPEG, PNG or WebP up to the"
              + " configured size. The `Location` header has the URL to read it.")
  @ApiResponse(responseCode = "201", description = "Image stored")
  @ApiResponse(
      responseCode = "400",
      description = "Missing, empty or not a JPEG, PNG or WebP image",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Not logged in",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "413",
      description = "The image is too large",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ImageDTO> upload(@RequestPart("file") MultipartFile file) {
    ImageDTO image = imageService.store(file);
    return ResponseEntity.created(URI.create("/api/v1/images/" + image.id())).body(image);
  }

  @Operation(
      summary = "Get the bytes of an image",
      description = "Public, so `<img>` tags can use it.")
  @ApiResponse(
      responseCode = "200",
      description = "The image, with its `Content-Type` and cache headers",
      content = @Content(mediaType = "image/*"))
  @ApiResponse(
      responseCode = "400",
      description = "The id is not a valid UUID",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No image has that id",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{id}")
  public ResponseEntity<byte[]> getImage(@PathVariable UUID id) {
    ImageContentDTO image = imageService.findContent(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.contentType()))
        .contentLength(image.data().length)
        .cacheControl(CACHE)
        .body(image.data());
  }
}
