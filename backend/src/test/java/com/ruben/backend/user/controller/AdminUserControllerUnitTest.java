package com.ruben.backend.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.user.dto.AdminUserDTO;
import com.ruben.backend.user.dto.UpdateProfileRequest;
import com.ruben.backend.user.dto.UserStatusRequest;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.service.UserService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Tag("B7")
@ExtendWith(MockitoExtension.class)
class AdminUserControllerUnitTest {

  private static final AdminUserDTO ANA =
      new AdminUserDTO(5L, "Ana", "ana@example.com", UserStatus.ACTIVE, LocalDate.of(2026, 10, 1));

  @Mock private UserService userService;
  @InjectMocks private AdminUserController controller;

  @Test
  void getUsersReturnsThePageOfTheService() {
    Pageable pageable = PageRequest.of(0, 10);
    PageDTO<AdminUserDTO> page = new PageDTO<>(List.of(ANA), 0, 10, 1, true);
    when(userService.findPage("ana", pageable)).thenReturn(page);

    assertThat(controller.getUsers("ana", pageable)).isEqualTo(page);
  }

  @Test
  void updateUserReturnsTheUpdatedUser() {
    UpdateProfileRequest request = new UpdateProfileRequest("Ana R", "ana@example.com");
    when(userService.updateByAdmin(5L, request)).thenReturn(ANA);

    assertThat(controller.updateUser(5L, request)).isEqualTo(ANA);
  }

  @Test
  void changeStatusPassesTheRequestedStatus() {
    AdminUserDTO blocked =
        new AdminUserDTO(5L, "Ana", "ana@example.com", UserStatus.BLOCKED, ANA.registrationDate());
    when(userService.changeStatus(5L, UserStatus.BLOCKED)).thenReturn(blocked);

    assertThat(controller.changeStatus(5L, new UserStatusRequest(UserStatus.BLOCKED)))
        .isEqualTo(blocked);
  }
}
