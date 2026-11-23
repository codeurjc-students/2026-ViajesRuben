package com.ruben.backend.participation.service;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.participation.dto.ParticipantDTO;
import com.ruben.backend.participation.entity.Participation;
import com.ruben.backend.participation.repository.ParticipationRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.service.TripAccessService;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.service.AuthService;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Managing who is in a trip (I3): everyone who can see the trip sees who joined it, the creator
 * removes participants, and a participant leaves. {@link TripAccessService} decides who may do
 * what; {@link ParticipationService} only answers membership questions, so the two do not depend on
 * each other in a cycle.
 *
 * <p>Removing or leaving deletes the participation and nothing else: the invitation stays ACCEPTED
 * as history and the person can be invited again. The next request of the removed user to the trip
 * is refused, because reading a trip checks the participation on every request.
 */
@Service
public class ParticipantService {

  private static final Logger log = LoggerFactory.getLogger(ParticipantService.class);

  /** The only properties a client may sort by. */
  private static final Set<String> SORTABLE = Set.of("id", "joinedAt");

  private final ParticipationRepository participationRepository;
  private final TripAccessService tripAccessService;
  private final AuthService authService;

  public ParticipantService(
      ParticipationRepository participationRepository,
      TripAccessService tripAccessService,
      AuthService authService) {
    this.participationRepository = participationRepository;
    this.tripAccessService = tripAccessService;
    this.authService = authService;
  }

  /**
   * One page of the people who joined the trip, for anyone who may see it. Only the trip's creator
   * gets their emails; for everyone else the email is null. The creator is not listed: they have no
   * participation.
   *
   * @throws BadRequestException the page asks to sort by a property that is not sortable
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user may not see the trip
   */
  @Transactional(readOnly = true)
  public PageDTO<ParticipantDTO> findPage(Long tripId, Pageable pageable) {
    pageable
        .getSort()
        .forEach(
            order -> {
              if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty() + "'");
              }
            });
    Trip trip = tripAccessService.findForRead(tripId);
    boolean isCreator = trip.getCreator().getId().equals(authService.getCurrentUser().id());
    return PageDTO.from(
        participationRepository
            .findParticipantsByTripId(tripId, pageable)
            .map(participant -> isCreator ? participant : participant.withoutEmail()));
  }

  /**
   * Removes a participant from the trip: the creator removes anyone, a participant only themselves,
   * which is leaving. Takes effect immediately.
   *
   * @throws ResourceNotFoundException no trip has that id, or the user is not one of its
   *     participants
   * @throws ForbiddenException the user may not remove that participant
   * @throws ConflictException the target is the trip's creator, who cannot leave their own trip
   */
  @Transactional
  public void remove(Long tripId, Long userId) {
    Trip trip = tripAccessService.findForParticipantRemoval(tripId, userId);
    UserDTO current = authService.getCurrentUser();
    if (trip.getCreator().getId().equals(userId)) {
      throw new ConflictException("The creator cannot leave the trip; delete it instead");
    }
    Participation participation =
        participationRepository
            .findByTripIdAndUserId(tripId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
    participationRepository.delete(participation);
    participationRepository.flush();
    if (current.id().equals(userId)) {
      log.info("User {} left trip {}", userId, tripId);
    } else {
      log.info("User {} removed user {} from trip {}", current.id(), userId, tripId);
    }
  }
}
