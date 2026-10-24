package com.ruben.backend.common.security;

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
class TripMateUserDetailsUnitTest {

  @Test
  void copiesTheIdentityOfTheUserAndUsesTheEmailAsUsername() {
    User user = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);

    TripMateUserDetails details = new TripMateUserDetails(user);

    assertThat(details.getId()).isEqualTo(7L);
    assertThat(details.getName()).isEqualTo("Test USER");
    assertThat(details.getUsername()).isEqualTo("ana@example.com");
    assertThat(details.getPassword()).isEqualTo("hash");
    assertThat(details.getRole()).isEqualTo(Role.USER);
    assertThat(details.getRegistrationDate()).isEqualTo(user.getRegistrationDate());
    assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
  }

  @Test
  void aUserWithoutAvatarHasNoAvatarId() {
    User user = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);

    assertThat(new TripMateUserDetails(user).getAvatarId()).isNull();
  }

  @Test
  void carriesTheAvatarIdWithoutTheImageBytes() {
    User user = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    Image avatar = new Image(ImageTestData.png(10), "image/png");
    UUID id = UUID.randomUUID();
    ReflectionTestUtils.setField(avatar, "id", id);
    user.setAvatar(avatar);

    assertThat(new TripMateUserDetails(user).getAvatarId()).isEqualTo(id);
  }

  @Test
  void onlyABlockedUserIsLocked() {
    User active = UserTestData.user(1L, "a@example.com", Role.USER, UserStatus.ACTIVE);
    User blocked = UserTestData.user(2L, "b@example.com", Role.USER, UserStatus.BLOCKED);

    assertThat(new TripMateUserDetails(active).isAccountNonLocked()).isTrue();
    assertThat(new TripMateUserDetails(blocked).isAccountNonLocked()).isFalse();
  }

  @Test
  void theAdministratorHasTheAdminAuthority() {
    User admin = UserTestData.user(1L, "admin@example.com", Role.ADMIN, UserStatus.ACTIVE);

    assertThat(new TripMateUserDetails(admin).getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_ADMIN");
  }
}
