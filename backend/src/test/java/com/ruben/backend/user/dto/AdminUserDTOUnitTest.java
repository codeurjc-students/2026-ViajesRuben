package com.ruben.backend.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("B7")
class AdminUserDTOUnitTest {

  @Test
  void fromCopiesTheDetailsAndTheStatus() {
    User user = UserTestData.user(5L, "ana@example.com", Role.USER, UserStatus.BLOCKED);

    AdminUserDTO dto = AdminUserDTO.from(user);

    assertThat(dto)
        .isEqualTo(
            new AdminUserDTO(
                5L,
                "Test USER",
                "ana@example.com",
                UserStatus.BLOCKED,
                user.getRegistrationDate()));
  }
}
