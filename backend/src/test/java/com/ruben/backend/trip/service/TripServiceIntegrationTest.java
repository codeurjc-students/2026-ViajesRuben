package com.ruben.backend.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.common.security.TripMateUserDetailsService;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.dto.ImageResult;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.dto.TripRequest;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("B4")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TripServiceIntegrationTest {

  @Autowired private TripService tripService;
  @Autowired private UserService userService;
  @Autowired private TripRepository tripRepository;
  @Autowired private ImageRepository imageRepository;
  @Autowired private TripMateUserDetailsService userDetailsService;
  @Autowired private DatabaseCleaner databaseCleaner;

  private UserDTO ana;
  private UserDTO bob;

  @BeforeEach
  void setUp() {
    databaseCleaner.clear();
    ana = userService.register(new RegisterRequest("Ana", "ana@example.com", "secret-pass"));
    bob = userService.register(new RegisterRequest("Bob", "bob@example.com", "secret-pass"));
    logInAs(ana);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  /** Makes the user the one of the current request, as the JWT filter would. */
  private void logInAs(UserDTO user) {
    TripMateUserDetails details = userDetailsService.loadUserById(user.id());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities()));
  }

  private static TripRequest request(String name, int dayOffset) {
    LocalDate start = LocalDate.of(2027, 1, 1).plusDays(dayOffset);
    return new TripRequest(name, "City", start, start.plusDays(2), null, null);
  }

  private void createTrips(int count, String prefix) {
    for (int i = 1; i <= count; i++) {
      tripService.create(request(prefix + " " + i, i));
    }
  }

  private static MockMultipartFile png() {
    return new MockMultipartFile("file", "a.png", "image/png", ImageTestData.png(100));
  }

  @Test
  void createPersistsTheTripForTheCurrentUserWithBudgetAndDescription() {
    TripDTO created =
        tripService.create(
            new TripRequest(
                "  Lisbon ",
                "Lisbon",
                LocalDate.of(2027, 5, 1),
                LocalDate.of(2027, 5, 5),
                new BigDecimal("1200.5"),
                "Pastéis"));

    Trip stored = tripRepository.findById(created.id()).orElseThrow();
    assertThat(stored.getCreator().getId()).isEqualTo(ana.id());
    assertThat(stored.getName()).isEqualTo("Lisbon");
    assertThat(stored.getBudget()).isEqualByComparingTo("1200.50");
    assertThat(stored.getDescription()).isEqualTo("Pastéis");
    assertThat(stored.getCreatedAt()).isNotNull();
    assertThat(stored.getCoverImage()).isNull();
  }

  @Test
  void findPageReturnsOnlyTheCurrentUsersTripsOrderedByStartDate() {
    createTrips(12, "Ana trip");
    logInAs(bob);
    createTrips(3, "Bob trip");
    logInAs(ana);

    PageDTO<TripDTO> first = tripService.findPage(PageRequest.of(0, 10, Sort.by("startDate")));
    PageDTO<TripDTO> second = tripService.findPage(PageRequest.of(1, 10, Sort.by("startDate")));

    assertThat(first.content()).hasSize(10);
    assertThat(first.content().get(0).name()).isEqualTo("Ana trip 1");
    assertThat(first.content().get(9).name()).isEqualTo("Ana trip 10");
    assertThat(first.totalElements()).isEqualTo(12);
    assertThat(first.last()).isFalse();
    assertThat(second.content())
        .extracting(TripDTO::name)
        .containsExactly("Ana trip 11", "Ana trip 12");
    assertThat(second.last()).isTrue();
    assertThat(first.content()).extracting(TripDTO::creatorId).containsOnly(ana.id());
  }

  @Test
  void findPageSortsDescendingInTheQuery() {
    createTrips(3, "Ana trip");

    PageDTO<TripDTO> page =
        tripService.findPage(PageRequest.of(0, 10, Sort.by("name").descending()));

    assertThat(page.content())
        .extracting(TripDTO::name)
        .containsExactly("Ana trip 3", "Ana trip 2", "Ana trip 1");
  }

  @Test
  void findPageRefusesToSortByTheCreatorsPassword() {
    assertThatThrownBy(
            () -> tripService.findPage(PageRequest.of(0, 10, Sort.by("creator.password"))))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void updateReplacesTheFieldsAndKeepsTheCover() {
    TripDTO created = tripService.create(request("Old", 1));
    tripService.replaceCover(created.id(), png());

    TripDTO updated =
        tripService.update(
            created.id(),
            new TripRequest(
                "New",
                "Oslo",
                LocalDate.of(2028, 2, 1),
                LocalDate.of(2028, 2, 3),
                new BigDecimal("10"),
                "Cold"));

    assertThat(updated.name()).isEqualTo("New");
    assertThat(updated.destination()).isEqualTo("Oslo");
    assertThat(updated.budget()).isEqualByComparingTo("10.00");
    assertThat(updated.coverImageId()).isNotNull();
    assertThat(tripService.findById(created.id()).description()).isEqualTo("Cold");
  }

  @Test
  void anotherUsersTripCannotBeReadChangedOrDeleted() {
    TripDTO created = tripService.create(request("Ana's", 1));
    logInAs(bob);

    assertThatThrownBy(() -> tripService.findById(created.id()))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> tripService.update(created.id(), request("Mine now", 1)))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> tripService.delete(created.id()))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> tripService.replaceCover(created.id(), png()))
        .isInstanceOf(ForbiddenException.class);
    assertThat(tripRepository.findById(created.id()).orElseThrow().getName()).isEqualTo("Ana's");
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void aMissingTripIsNotFound() {
    assertThatThrownBy(() -> tripService.findById(999_999L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deleteRemovesTheTripAndItsCoverImage() {
    TripDTO created = tripService.create(request("With cover", 1));
    tripService.replaceCover(created.id(), png());
    assertThat(imageRepository.count()).isEqualTo(1);

    tripService.delete(created.id());

    assertThat(tripRepository.existsById(created.id())).isFalse();
    assertThat(imageRepository.count()).isZero();
  }

  @Test
  void deleteOfATripWithoutCoverLeavesOtherTripsAndImagesAlone() {
    TripDTO kept = tripService.create(request("Kept", 1));
    tripService.replaceCover(kept.id(), png());
    TripDTO removed = tripService.create(request("Removed", 2));

    tripService.delete(removed.id());

    assertThat(tripRepository.existsById(kept.id())).isTrue();
    assertThat(imageRepository.count()).isEqualTo(1);
  }

  @Test
  void replacingTheCoverDeletesThePreviousImageRow() {
    TripDTO created = tripService.create(request("Cover", 1));
    ImageResult first = tripService.replaceCover(created.id(), png());
    ImageResult second = tripService.replaceCover(created.id(), png());

    assertThat(first.created()).isTrue();
    assertThat(second.created()).isFalse();
    assertThat(imageRepository.existsById(first.image().id())).isFalse();
    assertThat(imageRepository.existsById(second.image().id())).isTrue();
    assertThat(imageRepository.count()).isEqualTo(1);
    assertThat(tripService.findById(created.id()).coverImageId()).isEqualTo(second.image().id());
    assertThat(tripService.findCoverContent(created.id()).contentType()).isEqualTo("image/png");
  }

  @Test
  void deleteCoverClearsTheLinkAndTheImageRow() {
    TripDTO created = tripService.create(request("Cover", 1));
    tripService.replaceCover(created.id(), png());

    tripService.deleteCover(created.id());

    assertThat(tripService.findById(created.id()).coverImageId()).isNull();
    assertThat(imageRepository.count()).isZero();
    assertThatThrownBy(() -> tripService.findCoverContent(created.id()))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
