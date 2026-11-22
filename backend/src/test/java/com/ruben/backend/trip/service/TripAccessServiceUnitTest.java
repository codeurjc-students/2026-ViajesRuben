package com.ruben.backend.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.participation.service.ParticipationService;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.service.AuthService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("B5")
@ExtendWith(MockitoExtension.class)
class TripAccessServiceUnitTest {

  @Mock private TripRepository tripRepository;
  @Mock private AuthService authService;
  @Mock private ParticipationService participationService;

  private TripAccessService tripAccessService;
  private User ana;
  private User bob;

  @BeforeEach
  void setUp() {
    tripAccessService = new TripAccessService(tripRepository, authService, participationService);
    ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    bob = UserTestData.user(8L, "bob@example.com", Role.USER, UserStatus.ACTIVE);
  }

  private void loggedInAs(User user) {
    when(authService.getCurrentUser()).thenReturn(UserDTO.from(user));
  }

  private Trip tripOf(User creator) {
    Trip trip =
        new Trip(creator, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    ReflectionTestUtils.setField(trip, "id", 1L);
    when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
    return trip;
  }

  @Test
  void theCreatorMayReadAndChangeTheTrip() {
    loggedInAs(ana);
    Trip trip = tripOf(ana);

    assertThat(tripAccessService.findForRead(1L)).isSameAs(trip);
    assertThat(tripAccessService.findForWrite(1L)).isSameAs(trip);
  }

  @Test
  void anotherUserMayNeitherReadNorChangeTheTrip() {
    loggedInAs(ana);
    tripOf(bob);

    assertThatThrownBy(() -> tripAccessService.findForRead(1L))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("Only the creator can access this trip");
    assertThatThrownBy(() -> tripAccessService.findForWrite(1L))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("Only the creator can access this trip");
  }

  @Test
  @Tag("B8")
  void theCreatorMayAlsoDeleteTheTrip() {
    loggedInAs(ana);
    Trip trip = tripOf(ana);

    assertThat(tripAccessService.findForDelete(1L)).isSameAs(trip);
  }

  @Test
  @Tag("B8")
  void anotherUserMayNotDeleteTheTrip() {
    loggedInAs(ana);
    tripOf(bob);

    assertThatThrownBy(() -> tripAccessService.findForDelete(1L))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("Only the creator can access this trip");
  }

  @Test
  @Tag("B8")
  void theAdministratorMayReadAndDeleteAnotherUsersTrip() {
    User admin = UserTestData.user(1L, "admin@tripmate.com", Role.ADMIN, UserStatus.ACTIVE);
    loggedInAs(admin);
    Trip trip = tripOf(bob);

    assertThat(tripAccessService.findForRead(1L)).isSameAs(trip);
    assertThat(tripAccessService.findForDelete(1L)).isSameAs(trip);
  }

  @Test
  @Tag("B8")
  void theAdministratorMayNotChangeAnotherUsersTrip() {
    User admin = UserTestData.user(1L, "admin@tripmate.com", Role.ADMIN, UserStatus.ACTIVE);
    loggedInAs(admin);
    tripOf(bob);

    assertThatThrownBy(() -> tripAccessService.findForWrite(1L))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("Only the creator can access this trip");
  }

  @Test
  @Tag("I2")
  void aParticipantMayReadTheTrip() {
    loggedInAs(ana);
    Trip trip = tripOf(bob);
    when(participationService.isParticipant(1L, 7L)).thenReturn(true);

    assertThat(tripAccessService.findForRead(1L)).isSameAs(trip);
  }

  @Test
  @Tag("I2")
  void aParticipantMayNeitherChangeDeleteNorUnpublishTheTrip() {
    loggedInAs(ana);
    tripOf(bob);

    assertThatThrownBy(() -> tripAccessService.findForWrite(1L))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> tripAccessService.findForDelete(1L))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> tripAccessService.findForUnpublish(1L))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(participationService);
  }

  @Test
  @Tag("I2")
  void someoneWhoIsNotAParticipantMayNotReadTheTrip() {
    loggedInAs(ana);
    tripOf(bob);
    when(participationService.isParticipant(1L, 7L)).thenReturn(false);

    assertThatThrownBy(() -> tripAccessService.findForRead(1L))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("Only the creator can access this trip");
  }

  @Test
  @Tag("I2")
  void theParticipationsAreNotQueriedForTheCreatorOrTheAdministrator() {
    loggedInAs(ana);
    tripOf(ana);
    tripAccessService.findForRead(1L);
    User admin = UserTestData.user(1L, "admin@tripmate.com", Role.ADMIN, UserStatus.ACTIVE);
    loggedInAs(admin);

    tripAccessService.findForRead(1L);

    verifyNoInteractions(participationService);
  }

  @Test
  void aMissingTripIsNotFoundBeforeAnyPermissionIsChecked() {
    when(tripRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tripAccessService.findForRead(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Trip not found");
    assertThatThrownBy(() -> tripAccessService.findForDelete(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Trip not found");
    assertThatThrownBy(() -> tripAccessService.findForWrite(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Trip not found");
    verifyNoInteractions(authService);
  }

  @Test
  @Tag("B1")
  @Tag("I7")
  void aPublishedTripOfAnyUserIsFoundWithoutLookingAtTheCurrentUser() {
    Trip trip = new Trip(ana, "Rome", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    trip.publish();
    when(tripRepository.findByIdAndPublishedTrue(1L)).thenReturn(Optional.of(trip));

    assertThat(tripAccessService.findPublished(1L)).isSameAs(trip);
    verifyNoInteractions(authService);
  }

  @Test
  @Tag("B1")
  @Tag("I7")
  void aTripThatIsNotPublishedIsNotFoundAsPublicNeverForbidden() {
    when(tripRepository.findByIdAndPublishedTrue(2L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tripAccessService.findPublished(2L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Trip not found");
    verifyNoInteractions(authService);
  }

  @Test
  @Tag("I10")
  void theAdministratorMayUnpublishAnotherUsersTrip() {
    User admin = UserTestData.user(1L, "admin@tripmate.com", Role.ADMIN, UserStatus.ACTIVE);
    loggedInAs(admin);
    Trip trip = tripOf(ana);

    assertThat(tripAccessService.findForUnpublish(1L)).isSameAs(trip);
  }

  @Test
  @Tag("I10")
  void theCreatorMayUnpublishTheirOwnTrip() {
    loggedInAs(ana);
    Trip trip = tripOf(ana);

    assertThat(tripAccessService.findForUnpublish(1L)).isSameAs(trip);
  }

  @Test
  @Tag("I10")
  void anotherRegularUserMayNotUnpublishTheTrip() {
    loggedInAs(ana);
    tripOf(bob);

    assertThatThrownBy(() -> tripAccessService.findForUnpublish(1L))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("Only the creator can access this trip");
  }

  @Test
  @Tag("I10")
  void unpublishingAMissingTripIsNotFoundBeforeAnyPermissionIsChecked() {
    when(tripRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tripAccessService.findForUnpublish(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Trip not found");
    verifyNoInteractions(authService);
  }

  @Test
  @Tag("I7")
  void publishingIsDecidedLikeEditingSoOnlyTheCreatorMayDoIt() {
    User admin = UserTestData.user(1L, "admin@tripmate.com", Role.ADMIN, UserStatus.ACTIVE);
    loggedInAs(admin);
    tripOf(ana);

    assertThatThrownBy(() -> tripAccessService.findForWrite(1L))
        .isInstanceOf(ForbiddenException.class);
  }
}
