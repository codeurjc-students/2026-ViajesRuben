package com.ruben.backend.participation.service;

import com.ruben.backend.participation.entity.Participation;
import com.ruben.backend.participation.repository.ParticipationRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.entity.User;
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

  /** Adds the user to the trip; the caller has checked that they are not in it yet. */
  @Transactional
  public void add(Trip trip, User user) {
    participationRepository.saveAndFlush(new Participation(trip, user));
  }

  /** Deletes every participation of the trip; the trip's deletion calls it first (pattern 5). */
  @Transactional
  public void deleteAllByTrip(Long tripId) {
    participationRepository.deleteByTripId(tripId);
  }
}
