package com.ruben.backend.participation.repository;

import com.ruben.backend.participation.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

  boolean existsByTripIdAndUserId(Long tripId, Long userId);

  boolean existsByTripIdAndUserEmail(Long tripId, String email);

  /** Deletes every participation of a trip without loading them; used when the trip is deleted. */
  @Modifying(flushAutomatically = true)
  @Query("delete from Participation p where p.trip.id = :tripId")
  int deleteByTripId(@Param("tripId") Long tripId);
}
