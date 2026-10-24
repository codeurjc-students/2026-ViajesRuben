package com.ruben.backend.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.user.dto.AvatarResult;
import com.ruben.backend.user.dto.ChangePasswordRequest;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UpdateProfileRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.service.UserService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

@Tag("B2")
@Tag("B3")
@ExtendWith(MockitoExtension.class)
class UserControllerUnitTest {

  private static final UserDTO ANA =
      new UserDTO(5L, "Ana", "ana@example.com", Role.USER, null, LocalDate.of(2026, 10, 1));
  private static final MockMultipartFile PICTURE =
      new MockMultipartFile("file", "a.png", "image/png", ImageTestData.png(100));
  private static final ImageDTO AVATAR = new ImageDTO(UUID.randomUUID(), "image/png", 100);

  @Mock private UserService userService;
  @InjectMocks private UserController controller;

  @Test
  void registerAnswers201WithTheLocationAndTheUser() {
    RegisterRequest request = new RegisterRequest("Ana", "ana@example.com", "secret-pass");
    when(userService.register(request)).thenReturn(ANA);

    ResponseEntity<UserDTO> response = controller.register(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/users/5");
    assertThat(response.getBody()).isEqualTo(ANA);
  }

  @Test
  void getProfileReturnsTheProfileOfTheService() {
    when(userService.getProfile(5L)).thenReturn(ANA);

    assertThat(controller.getProfile(5L)).isEqualTo(ANA);
  }

  @Test
  void updateProfileReturnsTheUpdatedProfile() {
    UpdateProfileRequest request = new UpdateProfileRequest("Ana R", "ana@example.com");
    when(userService.updateProfile(5L, request)).thenReturn(ANA);

    assertThat(controller.updateProfile(5L, request)).isEqualTo(ANA);
  }

  @Test
  void changePasswordAnswers204() {
    ChangePasswordRequest request = new ChangePasswordRequest("old-secret", "new-secret");

    ResponseEntity<Void> response = controller.changePassword(5L, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(userService).changePassword(5L, request);
  }

  @Test
  void theFirstAvatarAnswers201WithItsLocation() {
    when(userService.replaceAvatar(5L, PICTURE)).thenReturn(new AvatarResult(AVATAR, true));

    ResponseEntity<ImageDTO> response = controller.replaceAvatar(5L, PICTURE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/users/5/avatar");
    assertThat(response.getBody()).isEqualTo(AVATAR);
  }

  @Test
  void aReplacedAvatarAnswers200WithoutLocation() {
    when(userService.replaceAvatar(5L, PICTURE)).thenReturn(new AvatarResult(AVATAR, false));

    ResponseEntity<ImageDTO> response = controller.replaceAvatar(5L, PICTURE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getLocation()).isNull();
    assertThat(response.getBody()).isEqualTo(AVATAR);
  }

  @Test
  void getAvatarAnswersTheBytesWithTheirTypeAndNoCache() {
    byte[] data = ImageTestData.png(50);
    when(userService.findAvatarContent(5L)).thenReturn(new ImageContentDTO(data, "image/png"));

    ResponseEntity<byte[]> response = controller.getAvatar(5L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
    assertThat(response.getHeaders().getContentLength()).isEqualTo(50);
    assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-cache");
    assertThat(response.getBody()).isEqualTo(data);
  }

  @Test
  void deleteAvatarAnswers204() {
    ResponseEntity<Void> response = controller.deleteAvatar(5L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(userService).deleteAvatar(5L);
  }
}
