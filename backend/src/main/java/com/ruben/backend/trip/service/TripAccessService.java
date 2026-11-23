package com.ruben.backend.trip.service;

import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.participation.service.ParticipationService;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place that decides who may see or change a trip. Every service that works on a trip or
 * on something that belongs to it (its cover, its activities) authorises through here and never
 * reads a trip's creator itself. The trip entity returned stays inside the services.
 */
@Service
public class TripAccessService {

  private final TripRepository tripRepository;
  private final AuthService authService;
  private final ParticipationService participationService;

  public TripAccessService(
      TripRepository tripRepository,
      AuthService authService,
      ParticipationService participationService) {
    this.tripRepository = tripRepository;
    this.authService = authService;
    this.participationService = participationService;
  }

  /**
   * Who may see a trip: its creator, the administrator, who reviews any trip (B8), and the
   * participants, who joined by accepting an invitation (I2). Reading never lets a participant
   * change anything. 404 comes before 403, so a missing trip is reported as missing to anyone.
   *
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user may not see the trip
   */
  @Transactional(readOnly = true)
  public Trip findForRead(Long id) {
    return find(id, true, true);
  }

  /**
   * Who may delete a trip: its creator, and the administrator, who removes inappropriate trips
   * (B8). Editing is a different rule, {@link #findForWrite}.
   *
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user may not delete the trip
   */
  @Transactional(readOnly = true)
  public Trip findForDelete(Long id) {
    return find(id, true, false);
  }

  /**
   * Who may unpublish a trip: its creator, and the administrator, who removes inappropriate public
   * itineraries without deleting the trip (I10). Publishing is a different rule, {@link
   * #findForWrite}.
   *
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user may not unpublish the trip
   */
  @Transactional(readOnly = true)
  public Trip findForUnpublish(Long id) {
    return find(id, true, false);
  }

  /**
   * Who may change a trip or anything that belongs to it (its cover, its activities): only its
   * creator, not even the administrator, who moderates but never edits other users' trips.
   *
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user may not change the trip
   */
  @Transactional(readOnly = true)
  public Trip findForWrite(Long id) {
    return find(id, false, false);
  }

  /**
   * A published trip is public to read, by anyone and without login. Any other trip is reported as
   * missing, never as forbidden, so the public endpoints reveal nothing about it.
   *
   * @throws ResourceNotFoundException no published trip has that id
   */
  @Transactional(readOnly = true)
  public Trip findPublished(Long id) {
    return tripRepository
        .findByIdAndPublishedTrue(id)
        .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
  }

  /**
   * Who may remove a participant (I3): the trip's creator may remove any of them, and a participant
   * only themselves (leaving). Nobody else, not even the administrator, who does not moderate
   * participants. Whether {@code userId} really participates is the caller's check, after this one,
   * so the order stays 404 trip, 403, then the rest.
   *
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user may not remove that participant
   */
  @Transactional(readOnly = true)
  public Trip findForParticipantRemoval(Long id, Long userId) {
    Trip trip = load(id);
    UserDTO current = authService.getCurrentUser();
    boolean isCreator = trip.getCreator().getId().equals(current.id());
    // The participation is only queried for someone leaving, never for the creator or a stranger.
    if (!isCreator
        && !(current.id().equals(userId) && participationService.isParticipant(id, current.id()))) {
      throw new ForbiddenException("Only the creator can remove other participants");
    }
    return trip;
  }

  private Trip load(Long id) {
    return tripRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
  }

  private Trip find(Long id, boolean administratorAllowed, boolean participantAllowed) {
    Trip trip = load(id);
    UserDTO current = authService.getCurrentUser();
    boolean isCreator = trip.getCreator().getId().equals(current.id());
    boolean isAllowedAdministrator = administratorAllowed && current.role() == Role.ADMIN;
    // The participations are only queried when the cheaper rules did not already allow the user.
    if (!isCreator
        && !isAllowedAdministrator
        && !(participantAllowed && participationService.isParticipant(id, current.id()))) {
      throw new ForbiddenException("Only the creator can access this trip");
    }
    return trip;
  }
}
