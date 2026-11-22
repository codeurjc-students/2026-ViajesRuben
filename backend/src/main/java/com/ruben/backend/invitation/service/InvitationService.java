package com.ruben.backend.invitation.service;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.invitation.dto.InvitationDTO;
import com.ruben.backend.invitation.dto.InvitationRequest;
import com.ruben.backend.invitation.dto.ReceivedInvitationDTO;
import com.ruben.backend.invitation.entity.Invitation;
import com.ruben.backend.invitation.entity.InvitationStatus;
import com.ruben.backend.invitation.repository.InvitationRepository;
import com.ruben.backend.notification.event.InvitationCreatedEvent;
import com.ruben.backend.participation.service.ParticipationService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The invitations of a trip. Only the trip's creator may invite and see them, decided through
 * {@link TripAccessService}; an invitation id of another trip is not found. The invited person,
 * identified by the normalised email of the logged-in account, sees and answers their own (I2). The
 * token of an invitation never leaves this package's entity. Invitations never expire.
 */
@Service
public class InvitationService {

  private static final Logger log = LoggerFactory.getLogger(InvitationService.class);

  /** The only properties a client may sort by. */
  private static final Set<String> SORTABLE = Set.of("id", "invitedEmail", "status", "sentAt");

  /** The only properties the addressee may sort the received invitations by. */
  private static final Set<String> SORTABLE_RECEIVED = Set.of("id", "sentAt");

  /** 32 random bytes, 43 characters once encoded: not guessable. */
  private static final int TOKEN_BYTES = 32;

  private static final SecureRandom RANDOM = new SecureRandom();

  private final InvitationRepository invitationRepository;
  private final UserRepository userRepository;
  private final TripAccessService tripAccessService;
  private final AuthService authService;
  private final ApplicationEventPublisher eventPublisher;
  private final ParticipationService participationService;

  public InvitationService(
      InvitationRepository invitationRepository,
      UserRepository userRepository,
      TripAccessService tripAccessService,
      AuthService authService,
      ApplicationEventPublisher eventPublisher,
      ParticipationService participationService) {
    this.invitationRepository = invitationRepository;
    this.userRepository = userRepository;
    this.tripAccessService = tripAccessService;
    this.authService = authService;
    this.eventPublisher = eventPublisher;
    this.participationService = participationService;
  }

  /**
   * Invites an email to the trip and publishes the event that sends it the email after the commit.
   * The email may have no account: that person registers and answers later (I2). Two simultaneous
   * identical requests may both pass the duplicate check; that race is accepted, the only effect
   * being a second pending invitation.
   *
   * @throws BadRequestException the email is the creator's own
   * @throws ConflictException the email already participates in the trip, or already has a pending
   *     invitation to it
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user is not the trip's creator
   */
  @Transactional
  public InvitationDTO create(Long tripId, InvitationRequest request) {
    Trip trip = tripAccessService.findForWrite(tripId);
    UserDTO current = authService.getCurrentUser();
    String email = normalizeEmail(request.email());
    ensureNotMember(current, trip, email);
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
    // Delivered to its listener only after this transaction commits: the email is never sent for an
    // invitation that was rolled back, and a failed email never undoes the invitation.
    eventPublisher.publishEvent(
        new InvitationCreatedEvent(
            invitation.getId(), email, invitation.getToken(), current.name(), trip.getName()));
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
    checkSortable(pageable, SORTABLE);
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

  /**
   * One page of the pending invitations addressed to the current user's email, newest first by
   * default. Invitations do not expire: one stays pending until it is answered or its trip is
   * deleted, which deletes it.
   *
   * @throws BadRequestException the page asks to sort by a property that is not sortable
   */
  @Transactional(readOnly = true)
  public PageDTO<ReceivedInvitationDTO> findMine(Pageable pageable) {
    checkSortable(pageable, SORTABLE_RECEIVED);
    String email = normalizeEmail(authService.getCurrentUser().email());
    return PageDTO.from(
        invitationRepository
            .findByInvitedEmailAndStatus(email, InvitationStatus.PENDING, pageable)
            .map(ReceivedInvitationDTO::from));
  }

  /**
   * The invitation the link of an email points at, in any status, for the account it was sent to.
   *
   * @throws ResourceNotFoundException no invitation has that token
   * @throws ForbiddenException the invitation is addressed to another email
   */
  @Transactional(readOnly = true)
  public ReceivedInvitationDTO findByToken(String token) {
    Invitation invitation =
        invitationRepository
            .findByToken(token)
            .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
    ensureInvitee(invitation, authService.getCurrentUser());
    return ReceivedInvitationDTO.from(invitation);
  }

  /**
   * The invited person accepts or rejects. Accepting makes them a participant of the trip and
   * remembers their account on the invitation, in the same transaction.
   *
   * @throws BadRequestException the answer is PENDING
   * @throws ResourceNotFoundException no invitation has that id (a deleted trip deletes its
   *     invitations)
   * @throws ForbiddenException the invitation is addressed to another email
   * @throws ConflictException the invitation was already answered, or the user is already in the
   *     trip
   */
  @Transactional
  public ReceivedInvitationDTO respond(Long id, InvitationStatus answer) {
    if (answer == InvitationStatus.PENDING) {
      throw new BadRequestException("The answer must be ACCEPTED or REJECTED");
    }
    Invitation invitation =
        invitationRepository
            .findWithDetailsById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
    UserDTO current = authService.getCurrentUser();
    ensureInvitee(invitation, current);
    if (invitation.getStatus() != InvitationStatus.PENDING) {
      throw new ConflictException("The invitation was already answered");
    }
    Trip trip = invitation.getTrip();
    if (answer == InvitationStatus.ACCEPTED) {
      if (trip.getCreator().getId().equals(current.id())
          || participationService.isParticipant(trip.getId(), current.id())) {
        throw new ConflictException("Already a participant");
      }
      User user = userRepository.getReferenceById(current.id());
      participationService.add(trip, user);
      invitation.accept(user);
    } else {
      invitation.reject();
    }
    invitationRepository.saveAndFlush(invitation);
    log.info(
        "User {} {} invitation {} to trip {}",
        current.id(),
        answer == InvitationStatus.ACCEPTED ? "accepted" : "rejected",
        id,
        trip.getId());
    return ReceivedInvitationDTO.from(invitation);
  }

  /** Deletes every invitation of the trip; the trip's deletion calls it first (pattern 5). */
  @Transactional
  public void deleteAllByTrip(Long tripId) {
    invitationRepository.deleteByTripId(tripId);
  }

  /**
   * Refuses an email that already belongs to the trip: the creator's own (400, the caller) or a
   * participant's (409). Every "already in the trip" rule of an invitation lives in this method.
   */
  private void ensureNotMember(UserDTO creator, Trip trip, String email) {
    if (email.equalsIgnoreCase(creator.email())) {
      throw new BadRequestException("You cannot invite yourself");
    }
    if (participationService.isParticipantByEmail(trip.getId(), email)) {
      throw new ConflictException("Already a participant");
    }
  }

  /** Only the account whose email the invitation was sent to may see it or answer it. */
  private static void ensureInvitee(Invitation invitation, UserDTO current) {
    if (!invitation.getInvitedEmail().equals(normalizeEmail(current.email()))) {
      throw new ForbiddenException("The invitation is addressed to another account");
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

  private static void checkSortable(Pageable pageable, Set<String> sortable) {
    pageable
        .getSort()
        .forEach(
            order -> {
              if (!sortable.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty() + "'");
              }
            });
  }
}
