package com.ruben.backend.user.controller;

import com.ruben.backend.common.error.ErrorResponse;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.user.dto.AvatarResult;
import com.ruben.backend.user.dto.ChangePasswordRequest;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UpdateProfileRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private static final String NOT_LOGGED_IN = "Not logged in";
  private static final String NOT_OWN_PROFILE = "It is another user's profile";
  private static final String NO_USER = "No user has that id";

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(
      summary = "Register a new account",
      description =
          "Public. The new user is always a regular user and is not logged in: call `POST"
              + " /api/v1/auth/login` afterwards. The `Location` header is the URL of the new"
              + " user's profile.")
  @ApiResponse(responseCode = "201", description = "The account was created")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid name, email or password",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "The email is already registered",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping
  public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
    UserDTO user = userService.register(request);
    return ResponseEntity.created(URI.create("/api/v1/users/" + user.id())).body(user);
  }

  @Operation(
      summary = "Get one's own profile",
      description =
          "Only the logged-in user can read their own profile. `avatarId` is null without avatar;"
              + " otherwise the picture is at `GET /api/v1/images/{avatarId}`.")
  @ApiResponse(responseCode = "200", description = "The profile")
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_OWN_PROFILE,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_USER,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{id}")
  public UserDTO getProfile(@PathVariable Long id) {
    return userService.getProfile(id);
  }

  @Operation(
      summary = "Edit one's own name and email",
      description =
          "Same rules as at registration. The session keeps working after an email change. The"
              + " administrator's details come from the configuration and cannot be edited.")
  @ApiResponse(responseCode = "200", description = "The updated profile")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid name or email",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "It is another user's profile, or the administrator's",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_USER,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "The email is already registered",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping("/{id}")
  public UserDTO updateProfile(
      @PathVariable Long id, @Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateProfile(id, request);
  }

  @Operation(
      summary = "Change one's own password",
      description =
          "Requires the current password; the new one has 8 to 72 characters. Sessions already"
              + " open stay valid until their token expires.")
  @ApiResponse(responseCode = "204", description = "The password was changed")
  @ApiResponse(
      responseCode = "400",
      description = "The current password is incorrect, or the new one is invalid",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "It is another user's profile, or the administrator's",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_USER,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping("/{id}/password")
  public ResponseEntity<Void> changePassword(
      @PathVariable Long id, @Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(id, request);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Upload or replace one's own avatar",
      description =
          "Multipart form with the picture in the `file` part: JPEG, PNG or WebP up to the"
              + " configured size. The previous avatar is deleted.")
  @ApiResponse(
      responseCode = "201",
      description = "The user had no avatar; `Location` is the avatar URL")
  @ApiResponse(responseCode = "200", description = "The previous avatar was replaced")
  @ApiResponse(
      responseCode = "400",
      description = "Missing, empty or not a JPEG, PNG or WebP image",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_OWN_PROFILE,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_USER,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "413",
      description = "The image is too large",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping(path = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ImageDTO> replaceAvatar(
      @PathVariable Long id, @RequestPart("file") MultipartFile file) {
    AvatarResult result = userService.replaceAvatar(id, file);
    if (result.created()) {
      return ResponseEntity.created(URI.create("/api/v1/users/" + id + "/avatar"))
          .body(result.image());
    }
    return ResponseEntity.ok(result.image());
  }

  @Operation(
      summary = "Get the bytes of a user's avatar",
      description =
          "Any logged-in user. Not cached, because it changes when the avatar is replaced; the"
              + " SPA uses `GET /api/v1/images/{avatarId}` instead.")
  @ApiResponse(
      responseCode = "200",
      description = "The avatar, with its `Content-Type`",
      content = @Content(mediaType = "image/*"))
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "No user has that id, or the user has no avatar",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{id}/avatar")
  public ResponseEntity<byte[]> getAvatar(@PathVariable Long id) {
    ImageContentDTO avatar = userService.findAvatarContent(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(avatar.contentType()))
        .contentLength(avatar.data().length)
        .cacheControl(CacheControl.noCache())
        .body(avatar.data());
  }

  @Operation(
      summary = "Delete one's own avatar",
      description = "Also answers 204 when there was no avatar.")
  @ApiResponse(responseCode = "204", description = "The user has no avatar now")
  @ApiResponse(
      responseCode = "401",
      description = NOT_LOGGED_IN,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = NOT_OWN_PROFILE,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = NO_USER,
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @DeleteMapping("/{id}/avatar")
  public ResponseEntity<Void> deleteAvatar(@PathVariable Long id) {
    userService.deleteAvatar(id);
    return ResponseEntity.noContent().build();
  }
}
