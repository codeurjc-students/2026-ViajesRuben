package com.ruben.backend.user.dto;

import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;

public record UserDTO(Long id, String name, String email, Role role) {

  public static UserDTO from(User user) {
    return new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
  }
}
