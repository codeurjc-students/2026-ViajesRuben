package com.ruben.backend.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("B3")
class UserDTOUnitTest {

  @Test
  void mapsTheUserWithoutAvatar() {
    User user = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);

    UserDTO dto = UserDTO.from(user);

    assertThat(dto)
        .isEqualTo(
            new UserDTO(
                7L, "Test USER", "ana@example.com", Role.USER, null, user.getRegistrationDate()));
  }

  @Test
  void mapsTheAvatarToItsImageId() {
    User user = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    Image avatar = new Image(ImageTestData.png(10), "image/png");
    UUID id = UUID.randomUUID();
    ReflectionTestUtils.setField(avatar, "id", id);
    user.setAvatar(avatar);

    assertThat(UserDTO.from(user).avatarId()).isEqualTo(id);
  }

  @Test
  void neverExposesThePasswordHash() {
    assertThat(UserDTO.class.getRecordComponents())
        .extracting("name")
        .containsExactly("id", "name", "email", "role", "avatarId", "registrationDate");
  }
}
