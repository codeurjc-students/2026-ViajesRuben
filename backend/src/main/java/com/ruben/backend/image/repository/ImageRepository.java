package com.ruben.backend.image.repository;

import com.ruben.backend.image.entity.Image;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, UUID> {}
