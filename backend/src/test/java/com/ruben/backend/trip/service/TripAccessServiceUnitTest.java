package com.ruben.backend.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
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

  private TripAccessService tripAccessService;
  private User ana;
  private User bob;

  @BeforeEach
  void setUp() {
    tripAccessService = new TripAccessService(tripRepository, authService);
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
  void theAdministratorHasNoAccessToAnotherUsersTripYet() {
    User admin = UserTestData.user(1L, "admin@tripmate.com", Role.ADMIN, UserStatus.ACTIVE);
    loggedInAs(admin);
    tripOf(bob);

    assertThatThrownBy(() -> tripAccessService.findForRead(1L))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> tripAccessService.findForWrite(1L))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void aMissingTripIsNotFoundBeforeAnyPermissionIsChecked() {
    when(tripRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tripAccessService.findForRead(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Trip not found");
    assertThatThrownBy(() -> tripAccessService.findForWrite(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Trip not found");
    verifyNoInteractions(authService);
  }
}
