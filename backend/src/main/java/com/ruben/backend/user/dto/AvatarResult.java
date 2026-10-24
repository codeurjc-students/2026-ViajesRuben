package com.ruben.backend.user.dto;

import com.ruben.backend.image.dto.ImageDTO;

/** The stored avatar, and whether the user had none before (201) or it replaced one (200). */
public record AvatarResult(ImageDTO image, boolean created) {}
