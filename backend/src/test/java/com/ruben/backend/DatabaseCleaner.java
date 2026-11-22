package com.ruben.backend;

import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.invitation.repository.InvitationRepository;
import com.ruben.backend.participation.repository.ParticipationRepository;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Empties the database for the system tests, in foreign-key order (participations and invitations
 * point at trips and users, activities at trips, users and images, trips at users, images and other
 * trips, users at images). A feature that adds an entity adds its repository here, once, and every
 * test that calls {@link #clear()} keeps working.
 */
@Component
public class DatabaseCleaner {

  private final InvitationRepository invitationRepository;
  private final ParticipationRepository participationRepository;
  private final ActivityRepository activityRepository;
  private final TripRepository tripRepository;
  private final UserRepository userRepository;
  private final ImageRepository imageRepository;
  private final TransactionTemplate transactions;

  @PersistenceContext private EntityManager entityManager;

  public DatabaseCleaner(
      InvitationRepository invitationRepository,
      ParticipationRepository participationRepository,
      ActivityRepository activityRepository,
      TripRepository tripRepository,
      UserRepository userRepository,
      ImageRepository imageRepository,
      TransactionTemplate transactions) {
    this.invitationRepository = invitationRepository;
    this.participationRepository = participationRepository;
    this.activityRepository = activityRepository;
    this.tripRepository = tripRepository;
    this.userRepository = userRepository;
    this.imageRepository = imageRepository;
    this.transactions = transactions;
  }

  public void clear() {
    participationRepository.deleteAll();
    invitationRepository.deleteAll();
    activityRepository.deleteAll();
    // A copy points at its original (I9): unlink them all before deleting any trip.
    transactions.executeWithoutResult(
        status ->
            entityManager.createQuery("update Trip t set t.copiedFrom = null").executeUpdate());
    tripRepository.deleteAll();
    userRepository.deleteAll();
    imageRepository.deleteAll();
  }
}
