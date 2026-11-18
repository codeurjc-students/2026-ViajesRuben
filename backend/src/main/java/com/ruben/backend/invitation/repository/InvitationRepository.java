package com.ruben.backend.invitation.repository;

import com.ruben.backend.invitation.entity.Invitation;
import com.ruben.backend.invitation.entity.InvitationStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

  boolean existsByTripIdAndInvitedEmailAndStatus(
      Long tripId, String invitedEmail, InvitationStatus status);

  Page<Invitation> findByTripId(Long tripId, Pageable pageable);

  Optional<Invitation> findByIdAndTripId(Long id, Long tripId);

  /** Deletes every invitation of a trip without loading them; used when the trip is deleted. */
  @Modifying(flushAutomatically = true)
  @Query("delete from Invitation i where i.trip.id = :tripId")
  int deleteByTripId(@Param("tripId") Long tripId);
}
