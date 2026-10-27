package com.ruben.backend.image.repository;

import com.ruben.backend.image.entity.Image;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, UUID> {

  /**
   * Deletes by id without reading the bytes first, as `deleteById` would. Pending changes are
   * flushed before, so a user that stopped pointing at the image is updated first.
   */
  @Modifying(flushAutomatically = true)
  @Query("delete from Image i where i.id = :id")
  int deleteWithoutLoading(@Param("id") UUID id);

  /** Same as {@link #deleteWithoutLoading} for several images at once. */
  @Modifying(flushAutomatically = true)
  @Query("delete from Image i where i.id in :ids")
  int deleteAllWithoutLoading(@Param("ids") Collection<UUID> ids);
}
