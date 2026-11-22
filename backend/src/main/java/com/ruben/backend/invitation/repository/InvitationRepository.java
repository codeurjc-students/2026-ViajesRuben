package com.ruben.backend.invitation.repository;

import com.ruben.backend.invitation.entity.Invitation;
import com.ruben.backend.invitation.entity.InvitationStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

  boolean existsByTripIdAndInvitedEmailAndStatus(
      Long tripId, String invitedEmail, InvitationStatus status);

  Page<Invitation> findByTripId(Long tripId, Pageable pageable);

  Optional<Invitation> findByIdAndTripId(Long id, Long tripId);

  /** An invitation with its trip and inviter, which the addressee's view shows. */
  @EntityGraph(attributePaths = {"trip", "inviter"})
  Optional<Invitation> findWithDetailsById(Long id);

  @EntityGraph(attributePaths = {"trip", "inviter"})
  Optional<Invitation> findByToken(String token);

  /** One page of the invitations addressed to an email in a status, with trip and inviter. */
  @EntityGraph(attributePaths = {"trip", "inviter"})
  Page<Invitation> findByInvitedEmailAndStatus(
      String invitedEmail, InvitationStatus status, Pageable pageable);

  /** Deletes every invitation of a trip without loading them; used when the trip is deleted. */
  @Modifying(flushAutomatically = true)
  @Query("delete from Invitation i where i.trip.id = :tripId")
  int deleteByTripId(@Param("tripId") Long tripId);
}
