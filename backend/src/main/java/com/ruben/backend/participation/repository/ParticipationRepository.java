package com.ruben.backend.participation.repository;

import com.ruben.backend.participation.dto.ParticipantDTO;
import com.ruben.backend.participation.dto.TripParticipantCount;
import com.ruben.backend.participation.entity.Participation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

  boolean existsByTripIdAndUserId(Long tripId, Long userId);

  boolean existsByTripIdAndUserEmail(Long tripId, String email);

  Optional<Participation> findByTripIdAndUserId(Long tripId, Long userId);

  long countByTripId(Long tripId);

  /**
   * The participants of each of the given trips in one grouped query, for a page of trips. A trip
   * nobody joined has no row.
   */
  @Query(
      "select new com.ruben.backend.participation.dto.TripParticipantCount(p.trip.id, count(p)) "
          + "from Participation p where p.trip.id in :tripIds group by p.trip.id")
  List<TripParticipantCount> countByTripIds(@Param("tripIds") Collection<Long> tripIds);

  /**
   * One page of the trip's participants, projected straight into the DTO: no entity is loaded, so
   * there is no N+1 and the avatar's bytes are never read, only its id. Paging and sorting happen
   * in the query.
   */
  @Query(
      value =
          "select new com.ruben.backend.participation.dto.ParticipantDTO("
              + "u.id, u.name, a.id, u.email, p.joinedAt) "
              + "from Participation p join p.user u left join u.avatar a "
              + "where p.trip.id = :tripId",
      countQuery = "select count(p) from Participation p where p.trip.id = :tripId")
  Page<ParticipantDTO> findParticipantsByTripId(@Param("tripId") Long tripId, Pageable pageable);

  /** Deletes every participation of a trip without loading them; used when the trip is deleted. */
  @Modifying(flushAutomatically = true)
  @Query("delete from Participation p where p.trip.id = :tripId")
  int deleteByTripId(@Param("tripId") Long tripId);
}
