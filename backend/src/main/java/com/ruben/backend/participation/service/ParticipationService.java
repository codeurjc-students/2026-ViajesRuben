package com.ruben.backend.participation.service;

import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.participation.entity.Participation;
import com.ruben.backend.participation.repository.ParticipationRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.entity.User;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who took part in a trip besides its creator. Other services ask here and never read the
 * participations themselves; I3 adds listing and removing them.
 */
@Service
public class ParticipationService {

  private final ParticipationRepository participationRepository;

  public ParticipationService(ParticipationRepository participationRepository) {
    this.participationRepository = participationRepository;
  }

  @Transactional(readOnly = true)
  public boolean isParticipant(Long tripId, Long userId) {
    return participationRepository.existsByTripIdAndUserId(tripId, userId);
  }

  /** Whether the account with this (normalised) email participates in the trip. */
  @Transactional(readOnly = true)
  public boolean isParticipantByEmail(Long tripId, String email) {
    return participationRepository.existsByTripIdAndUserEmail(tripId, email);
  }

  /** How many people joined the trip; the creator is not one of them. */
  @Transactional(readOnly = true)
  public long countByTrip(Long tripId) {
    return participationRepository.countByTripId(tripId);
  }

  /**
   * How many people joined each of the trips, in one query. Every given trip has an entry, 0 when
   * nobody joined it, so the caller needs no default.
   */
  @Transactional(readOnly = true)
  public Map<Long, Long> countByTrips(Collection<Long> tripIds) {
    Map<Long, Long> counts = new HashMap<>();
    tripIds.forEach(id -> counts.put(id, 0L));
    if (!tripIds.isEmpty()) {
      participationRepository
          .countByTripIds(tripIds)
          .forEach(row -> counts.put(row.tripId(), row.count()));
    }
    return counts;
  }

  /**
   * Adds the user to the trip. The caller has checked that they are not in it yet, but two
   * simultaneous requests can both pass that check: the unique constraint then refuses the second
   * one, which is reported as a conflict instead of a server error.
   *
   * @throws ConflictException the user already participates in the trip
   */
  @Transactional
  public void add(Trip trip, User user) {
    try {
      participationRepository.saveAndFlush(new Participation(trip, user));
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("Already a participant");
    }
  }

  /** Deletes every participation of the trip; the trip's deletion calls it first (pattern 5). */
  @Transactional
  public void deleteAllByTrip(Long tripId) {
    participationRepository.deleteByTripId(tripId);
  }
}
