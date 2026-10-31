package com.ruben.backend.user.dto;

import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.time.LocalDate;

/**
 * A user as the administrator manages them: account details and status. Never the password, and no
 * avatar, so listing users never touches the images.
 */
public record AdminUserDTO(
    Long id, String name, String email, UserStatus status, LocalDate registrationDate) {

  public static AdminUserDTO from(User user) {
    return new AdminUserDTO(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getStatus(),
        user.getRegistrationDate());
  }
}
