package com.ruben.backend.trip.service;

import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
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

  public TripAccessService(TripRepository tripRepository, AuthService authService) {
    this.tripRepository = tripRepository;
    this.authService = authService;
  }

  /**
   * Who may see a trip. Today only its creator; participants (I3) are added here, in one place. 404
   * comes before 403, so a missing trip is reported as missing to anyone.
   *
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user may not see the trip
   */
  @Transactional(readOnly = true)
  public Trip findForRead(Long id) {
    return findCreatedByCurrentUser(id);
  }

  /**
   * Who may change or delete a trip. Only its creator; the administrator may delete any trip once
   * moderation exists (B8), and that rule is added here.
   *
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user may not change the trip
   */
  @Transactional(readOnly = true)
  public Trip findForWrite(Long id) {
    return findCreatedByCurrentUser(id);
  }

  /**
   * An example trip is public to read: one the administrator created. Any other trip is reported as
   * missing, never as forbidden, so the public endpoints reveal nothing about it.
   *
   * @throws ResourceNotFoundException no example trip has that id
   */
  @Transactional(readOnly = true)
  public Trip findExample(Long id) {
    return tripRepository
        .findByIdAndCreatorRole(id, Role.ADMIN)
        .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
  }

  private Trip findCreatedByCurrentUser(Long id) {
    Trip trip =
        tripRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    if (!trip.getCreator().getId().equals(authService.getCurrentUser().id())) {
      throw new ForbiddenException("Only the creator can access this trip");
    }
    return trip;
  }
}
