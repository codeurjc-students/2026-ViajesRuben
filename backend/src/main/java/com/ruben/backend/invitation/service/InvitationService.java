package com.ruben.backend.invitation.service;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.invitation.dto.InvitationDTO;
import com.ruben.backend.invitation.dto.InvitationRequest;
import com.ruben.backend.invitation.entity.Invitation;
import com.ruben.backend.invitation.entity.InvitationStatus;
import com.ruben.backend.invitation.repository.InvitationRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.service.TripAccessService;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AuthService;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The invitations of a trip. Only the trip's creator may invite and see them, decided through
 * {@link TripAccessService}; an invitation id of another trip is not found. The token of an
 * invitation never leaves this package's entity.
 */
@Service
public class InvitationService {

  private static final Logger log = LoggerFactory.getLogger(InvitationService.class);

  /** The only properties a client may sort by. */
  private static final Set<String> SORTABLE = Set.of("id", "invitedEmail", "status", "sentAt");

  /** 32 random bytes, 43 characters once encoded: not guessable. */
  private static final int TOKEN_BYTES = 32;

  private static final SecureRandom RANDOM = new SecureRandom();

  private final InvitationRepository invitationRepository;
  private final UserRepository userRepository;
  private final TripAccessService tripAccessService;
  private final AuthService authService;

  public InvitationService(
      InvitationRepository invitationRepository,
      UserRepository userRepository,
      TripAccessService tripAccessService,
      AuthService authService) {
    this.invitationRepository = invitationRepository;
    this.userRepository = userRepository;
    this.tripAccessService = tripAccessService;
    this.authService = authService;
  }

  /**
   * Invites an email to the trip. The email may have no account: that person registers and answers
   * later (I2). Two simultaneous identical requests may both pass the duplicate check; that race is
   * accepted, the only effect being a second pending invitation.
   *
   * @throws BadRequestException the email is the creator's own
   * @throws ConflictException the email already has a pending invitation to the trip
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user is not the trip's creator
   */
  @Transactional
  public InvitationDTO create(Long tripId, InvitationRequest request) {
    Trip trip = tripAccessService.findForWrite(tripId);
    UserDTO current = authService.getCurrentUser();
    String email = normalizeEmail(request.email());
    ensureNotMember(current, email);
    if (invitationRepository.existsByTripIdAndInvitedEmailAndStatus(
        tripId, email, InvitationStatus.PENDING)) {
      throw new ConflictException("The email already has a pending invitation to this trip");
    }
    User invitedUser = userRepository.findByEmail(email).orElse(null);
    Invitation invitation =
        invitationRepository.saveAndFlush(
            new Invitation(
                trip,
                userRepository.getReferenceById(current.id()),
                email,
                invitedUser,
                newToken()));
    log.info(
        "User {} invited someone to trip {} (invitation {})",
        current.id(),
        tripId,
        invitation.getId());
    return InvitationDTO.from(invitation);
  }

  /**
   * One page of the trip's invitations.
   *
   * @throws BadRequestException the page asks to sort by a property that is not sortable
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user is not the trip's creator
   */
  @Transactional(readOnly = true)
  public PageDTO<InvitationDTO> findPage(Long tripId, Pageable pageable) {
    checkSortable(pageable);
    tripAccessService.findForWrite(tripId);
    return PageDTO.from(
        invitationRepository.findByTripId(tripId, pageable).map(InvitationDTO::from));
  }

  /**
   * @throws ResourceNotFoundException no trip has that id, or it has no such invitation
   * @throws ForbiddenException the user is not the trip's creator
   */
  @Transactional(readOnly = true)
  public InvitationDTO findById(Long tripId, Long id) {
    tripAccessService.findForWrite(tripId);
    return invitationRepository
        .findByIdAndTripId(id, tripId)
        .map(InvitationDTO::from)
        .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
  }

  /** Deletes every invitation of the trip; the trip's deletion calls it first (pattern 5). */
  @Transactional
  public void deleteAllByTrip(Long tripId) {
    invitationRepository.deleteByTripId(tripId);
  }

  /**
   * Refuses an email that already belongs to the trip. Today only its creator does, who is the
   * caller. I2 adds here, with the participations, the trip as a parameter and the check that the
   * email is not a participant (409), so every "already in the trip" rule stays in this one method.
   */
  private static void ensureNotMember(UserDTO creator, String email) {
    if (email.equalsIgnoreCase(creator.email())) {
      throw new BadRequestException("You cannot invite yourself");
    }
  }

  private static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  static String newToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static void checkSortable(Pageable pageable) {
    pageable
        .getSort()
        .forEach(
            order -> {
              if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty() + "'");
              }
            });
  }
}
