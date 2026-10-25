package com.ruben.backend.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Tag("B4")
@ExtendWith(MockitoExtension.class)
class DataLoaderUnitTest {

  @Mock private UserRepository userRepository;
  @Mock private TripRepository tripRepository;
  @InjectMocks private DataLoader dataLoader;
  @Captor private ArgumentCaptor<User> savedUser;
  @Captor private ArgumentCaptor<Iterable<Trip>> savedTrips;

  private static User withId(User user, long id) {
    return UserTestData.user(id, user.getEmail(), Role.USER, UserStatus.ACTIVE);
  }

  @Test
  void theDocumentedPasswordMatchesTheStoredHash() {
    assertThat(new BCryptPasswordEncoder().matches("Demo-2026", DataLoader.SAMPLE_USERS_HASH))
        .isTrue();
  }

  @Test
  void createsTheThreeSampleUsersAsActiveRegularUsersInAnEmptyDatabase() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.save(savedUser.capture()))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), 5L));
    when(tripRepository.findByCreatorIdAndNameIn(eq(5L), anyCollection())).thenReturn(List.of());

    dataLoader.run();

    assertThat(savedUser.getAllValues())
        .extracting(User::getEmail)
        .containsExactly("ana@tripmate.com", "carlos@tripmate.com", "laura@tripmate.com");
    assertThat(savedUser.getAllValues())
        .allSatisfy(
            user -> {
              assertThat(user.getRole()).isEqualTo(Role.USER);
              assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
              assertThat(user.getPassword()).isEqualTo(DataLoader.SAMPLE_USERS_HASH);
            });
  }

  @Test
  void assignsEverySampleTripToItsUser() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.save(savedUser.capture()))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              long id = user.getEmail().startsWith("ana") ? 1L : 2L;
              return withId(user, id);
            });
    when(tripRepository.findByCreatorIdAndNameIn(anyLong(), anyCollection())).thenReturn(List.of());

    dataLoader.run();

    verify(tripRepository, times(3)).saveAll(savedTrips.capture());
    List<Trip> anas = tripsOf(savedTrips.getAllValues().get(0));
    assertThat(anas).hasSize(12);
    assertThat(anas).extracting(trip -> trip.getCreator().getId()).containsOnly(1L);
    assertThat(anas).extracting(Trip::getDestination).contains("Lisbon", "Rome", "Tokyo");
    assertThat(tripsOf(savedTrips.getAllValues().get(1))).hasSize(3);
    assertThat(tripsOf(savedTrips.getAllValues().get(2))).hasSize(2);
    assertThat(anas)
        .allSatisfy(trip -> assertThat(trip.getEndDate()).isAfterOrEqualTo(trip.getStartDate()));
  }

  @Test
  void onlyAddsWhatIsMissingAndKeepsExistingUsers() {
    User ana = UserTestData.user(1L, "ana@tripmate.com", Role.USER, UserStatus.ACTIVE);
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByEmail("ana@tripmate.com")).thenReturn(Optional.of(ana));
    when(userRepository.save(savedUser.capture()))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), 2L));
    when(tripRepository.findByCreatorIdAndNameIn(eq(1L), anyCollection()))
        .thenReturn(
            List.of(
                new Trip(
                    ana,
                    "Lisbon getaway",
                    "Lisboa",
                    LocalDate.of(2027, 5, 1),
                    LocalDate.of(2027, 5, 5))));
    when(tripRepository.findByCreatorIdAndNameIn(eq(2L), anyCollection())).thenReturn(List.of());

    dataLoader.run();

    assertThat(savedUser.getAllValues())
        .extracting(User::getEmail)
        .doesNotContain("ana@tripmate.com");
    verify(tripRepository, times(3)).saveAll(savedTrips.capture());
    List<Trip> anas = tripsOf(savedTrips.getAllValues().get(0));
    assertThat(anas).hasSize(11);
    assertThat(anas).extracting(Trip::getName).doesNotContain("Lisbon getaway");
  }

  @Test
  void aSecondRunWithEverythingLoadedAddsNothing() {
    when(userRepository.findByEmail(anyString()))
        .thenAnswer(
            invocation ->
                Optional.of(
                    UserTestData.user(
                        1L, invocation.getArgument(0), Role.USER, UserStatus.ACTIVE)));
    when(tripRepository.findByCreatorIdAndNameIn(eq(1L), anyCollection()))
        .thenAnswer(
            invocation -> {
              Iterable<String> names = invocation.getArgument(1);
              User creator = UserTestData.user(1L, "x@example.com", Role.USER, UserStatus.ACTIVE);
              List<Trip> existing = new java.util.ArrayList<>();
              for (String name : names) {
                existing.add(
                    new Trip(
                        creator, name, "City", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 2)));
              }
              return existing;
            });

    dataLoader.run();

    verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    verify(tripRepository, times(3)).saveAll(savedTrips.capture());
    assertThat(savedTrips.getAllValues()).allSatisfy(trips -> assertThat(trips).isEmpty());
  }

  private static List<Trip> tripsOf(Iterable<Trip> trips) {
    List<Trip> list = new java.util.ArrayList<>();
    trips.forEach(list::add);
    return list;
  }
}
