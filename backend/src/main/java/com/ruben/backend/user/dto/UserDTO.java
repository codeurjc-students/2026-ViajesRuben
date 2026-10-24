package com.ruben.backend.user.dto;

import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What the API says about a user; never the password. The avatar bytes are served by `GET
 * /api/v1/images/{avatarId}`, and `avatarId` is null when the user has no avatar.
 */
public record UserDTO(
    Long id, String name, String email, Role role, UUID avatarId, LocalDate registrationDate) {

  public static UserDTO from(User user) {
    return new UserDTO(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getRole(),
        user.getAvatar() == null ? null : user.getAvatar().getId(),
        user.getRegistrationDate());
  }
}
