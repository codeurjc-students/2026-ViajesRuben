package com.ruben.backend.user.dto;

import com.ruben.backend.user.entity.Role;

public record UserDTO(Long id, String name, String email, Role role) {}
