package com.ruben.backend.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageResult;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.image.service.ImageService;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.dto.TripRequest;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AuthService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("B4")
@ExtendWith(MockitoExtension.class)
class TripServiceUnitTest {

  private static final MockMultipartFile PICTURE =
      new MockMultipartFile("file", "a.png", "image/png", ImageTestData.png(100));
  private static final TripRequest REQUEST =
      new TripRequest(
          "  Lisbon trip ",
          " Lisbon ",
          LocalDate.of(2027, 5, 1),
          LocalDate.of(2027, 5, 5),
          new BigDecimal("1200.50"),
          "  Pastéis de nata  ");

  @Mock private TripRepository tripRepository;
  @Mock private UserRepository userRepository;
  @Mock private AuthService authService;
  @Mock private ImageService imageService;

  private TripService tripService;
  private User ana;
  private User bob;

  @BeforeEach
  void setUp() {
    tripService = new TripService(tripRepository, userRepository, authService, imageService);
    ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    bob = UserTestData.user(8L, "bob@example.com", Role.USER, UserStatus.ACTIVE);
  }

  /** Ana is the logged-in user. */
  private void anaIsLoggedIn() {
    when(authService.getCurrentUser()).thenReturn(UserDTO.from(ana));
  }

  private static Trip newTrip(User creator, Long id) {
    Trip trip =
        new Trip(creator, "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17));
    ReflectionTestUtils.setField(trip, "id", id);
    return trip;
  }

  /** A trip of the given creator that the repository finds by id. */
  private Trip tripOf(User creator, Long id) {
    Trip trip = newTrip(creator, id);
    when(tripRepository.findById(id)).thenReturn(Optional.of(trip));
    return trip;
  }

  private static Image image() {
    Image image = new Image(ImageTestData.png(10), "image/png");
    ReflectionTestUtils.setField(image, "id", UUID.randomUUID());
    return image;
  }

  // --- list ---

  @Test
  void findPageReturnsTheCurrentUsersTripsKeepingThePosition() {
    anaIsLoggedIn();
    Pageable pageable = PageRequest.of(1, 2, Sort.by("startDate"));
    Trip first = newTrip(ana, 1L);
    Trip second = newTrip(ana, 2L);
    when(tripRepository.findByCreatorId(7L, pageable))
        .thenReturn(new PageImpl<>(List.of(first, second), pageable, 4));

    PageDTO<TripDTO> page = tripService.findPage(pageable);

    assertThat(page.content()).extracting(TripDTO::id).containsExactly(1L, 2L);
    assertThat(page.content()).extracting(TripDTO::creatorId).containsOnly(7L);
    assertThat(page.page()).isEqualTo(1);
    assertThat(page.size()).isEqualTo(2);
    assertThat(page.totalElements()).isEqualTo(4);
    assertThat(page.last()).isTrue();
  }

  @Test
  void findPageAcceptsEverySortableProperty() {
    anaIsLoggedIn();
    Pageable pageable =
        PageRequest.of(
            0,
            10,
            Sort.by("id", "name", "destination", "startDate", "endDate", "createdAt").descending());
    when(tripRepository.findByCreatorId(7L, pageable)).thenReturn(new PageImpl<>(List.of()));

    assertThat(tripService.findPage(pageable).content()).isEmpty();
  }

  @Test
  void findPageRejectsSortingByANestedOrUnknownProperty() {
    Pageable nested = PageRequest.of(0, 10, Sort.by("creator.password"));

    assertThatThrownBy(() -> tripService.findPage(nested))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot sort by 'creator.password'");
    verifyNoInteractions(tripRepository);
  }

  // --- create ---

  @Test
  void createStoresTheTripForTheCurrentUserWithTrimmedText() {
    anaIsLoggedIn();
    when(userRepository.getReferenceById(7L)).thenReturn(ana);
    when(tripRepository.saveAndFlush(any(Trip.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TripDTO created = tripService.create(REQUEST);

    assertThat(created.creatorId()).isEqualTo(7L);
    assertThat(created.name()).isEqualTo("Lisbon trip");
    assertThat(created.destination()).isEqualTo("Lisbon");
    assertThat(created.startDate()).isEqualTo(LocalDate.of(2027, 5, 1));
    assertThat(created.endDate()).isEqualTo(LocalDate.of(2027, 5, 5));
    assertThat(created.budget()).isEqualByComparingTo("1200.50");
    assertThat(created.description()).isEqualTo("Pastéis de nata");
    assertThat(created.coverImageId()).isNull();
  }

  @Test
  void createKeepsTheOptionalFieldsEmptyWhenTheyAreMissingOrBlank() {
    anaIsLoggedIn();
    when(userRepository.getReferenceById(7L)).thenReturn(ana);
    when(tripRepository.saveAndFlush(any(Trip.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TripDTO created =
        tripService.create(
            new TripRequest(
                "Trip", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 10), null, "   "));

    assertThat(created.budget()).isNull();
    assertThat(created.description()).isNull();
  }

  // --- read ---

  @Test
  void findByIdReturnsTheTripToItsCreator() {
    anaIsLoggedIn();
    tripOf(ana, 1L);

    TripDTO trip = tripService.findById(1L);

    assertThat(trip.name()).isEqualTo("Trip");
    assertThat(trip.destination()).isEqualTo("Rome");
  }

  @Test
  void findByIdThrowsNotFoundWhenTheTripDoesNotExist() {
    when(tripRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tripService.findById(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Trip not found");
    verifyNoInteractions(authService);
  }

  @Test
  void findByIdOfAnotherUsersTripIsForbidden() {
    anaIsLoggedIn();
    tripOf(bob, 1L);

    assertThatThrownBy(() -> tripService.findById(1L))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("Only the creator can access this trip");
  }

  @Test
  void theAdministratorCannotReadAnotherUsersTripYet() {
    User admin = UserTestData.user(1L, "admin@tripmate.com", Role.ADMIN, UserStatus.ACTIVE);
    when(authService.getCurrentUser()).thenReturn(UserDTO.from(admin));
    tripOf(bob, 1L);

    assertThatThrownBy(() -> tripService.findById(1L)).isInstanceOf(ForbiddenException.class);
  }

  // --- update ---

  @Test
  void updateReplacesEveryEditableFieldAndKeepsTheCover() {
    anaIsLoggedIn();
    Trip trip = tripOf(ana, 1L);
    Image cover = image();
    trip.setCoverImage(cover);
    trip.setBudget(new BigDecimal("5.00"));
    when(tripRepository.saveAndFlush(trip)).thenReturn(trip);

    TripDTO updated = tripService.update(1L, REQUEST);

    assertThat(updated.name()).isEqualTo("Lisbon trip");
    assertThat(updated.destination()).isEqualTo("Lisbon");
    assertThat(updated.startDate()).isEqualTo(LocalDate.of(2027, 5, 1));
    assertThat(updated.endDate()).isEqualTo(LocalDate.of(2027, 5, 5));
    assertThat(updated.budget()).isEqualByComparingTo("1200.50");
    assertThat(updated.description()).isEqualTo("Pastéis de nata");
    assertThat(updated.coverImageId()).isEqualTo(cover.getId());
  }

  @Test
  void updateOfAnotherUsersTripIsForbiddenAndWritesNothing() {
    anaIsLoggedIn();
    Trip trip = tripOf(bob, 1L);

    assertThatThrownBy(() -> tripService.update(1L, REQUEST))
        .isInstanceOf(ForbiddenException.class);
    assertThat(trip.getName()).isEqualTo("Trip");
    verify(tripRepository, never()).saveAndFlush(any());
  }

  @Test
  void updateOfAMissingTripIsNotFound() {
    when(tripRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tripService.update(99L, REQUEST))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // --- delete ---

  @Test
  void deleteRemovesTheTripAndThenItsCoverImage() {
    anaIsLoggedIn();
    Trip trip = tripOf(ana, 1L);
    Image cover = image();
    trip.setCoverImage(cover);

    tripService.delete(1L);

    InOrder order = Mockito.inOrder(tripRepository, imageService);
    order.verify(tripRepository).delete(trip);
    order.verify(imageService).delete(cover.getId());
  }

  @Test
  void deleteOfATripWithoutCoverDoesNotTouchImages() {
    anaIsLoggedIn();
    Trip trip = tripOf(ana, 1L);

    tripService.delete(1L);

    verify(tripRepository).delete(trip);
    verifyNoInteractions(imageService);
  }

  @Test
  void deleteOfAnotherUsersTripIsForbiddenAndDeletesNothing() {
    anaIsLoggedIn();
    Trip trip = tripOf(bob, 1L);
    trip.setCoverImage(image());

    assertThatThrownBy(() -> tripService.delete(1L)).isInstanceOf(ForbiddenException.class);
    verify(tripRepository, never()).delete(any());
    verifyNoInteractions(imageService);
  }

  @Test
  void deleteOfAMissingTripIsNotFound() {
    when(tripRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tripService.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
  }

  // --- cover ---

  @Test
  void theFirstCoverIsStoredAndReportedAsCreated() {
    anaIsLoggedIn();
    Trip trip = tripOf(ana, 1L);
    Image stored = image();
    when(imageService.save(PICTURE)).thenReturn(stored);

    ImageResult result = tripService.replaceCover(1L, PICTURE);

    assertThat(result.created()).isTrue();
    assertThat(result.image().id()).isEqualTo(stored.getId());
    assertThat(trip.getCoverImage()).isSameAs(stored);
    verify(tripRepository).save(trip);
    verify(imageService, never()).delete(any());
  }

  @Test
  void replacingTheCoverDeletesThePreviousImage() {
    anaIsLoggedIn();
    Trip trip = tripOf(ana, 1L);
    Image previous = image();
    trip.setCoverImage(previous);
    Image stored = image();
    when(imageService.save(PICTURE)).thenReturn(stored);

    ImageResult result = tripService.replaceCover(1L, PICTURE);

    assertThat(result.created()).isFalse();
    assertThat(trip.getCoverImage()).isSameAs(stored);
    verify(imageService).delete(previous.getId());
  }

  @Test
  void replacingTheCoverOfAnotherUsersTripIsForbiddenAndStoresNothing() {
    anaIsLoggedIn();
    tripOf(bob, 1L);

    assertThatThrownBy(() -> tripService.replaceCover(1L, PICTURE))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(imageService);
  }

  @Test
  void aRejectedImageLeavesTheCoverUntouched() {
    anaIsLoggedIn();
    Trip trip = tripOf(ana, 1L);
    when(imageService.save(PICTURE)).thenThrow(new BadRequestException("The image is empty"));

    assertThatThrownBy(() -> tripService.replaceCover(1L, PICTURE))
        .isInstanceOf(BadRequestException.class);
    assertThat(trip.getCoverImage()).isNull();
    verify(tripRepository, never()).save(any());
  }

  @Test
  void deleteCoverUnlinksAndDeletesTheImage() {
    anaIsLoggedIn();
    Trip trip = tripOf(ana, 1L);
    Image cover = image();
    trip.setCoverImage(cover);

    tripService.deleteCover(1L);

    assertThat(trip.getCoverImage()).isNull();
    verify(tripRepository).save(trip);
    verify(imageService).delete(cover.getId());
  }

  @Test
  void deleteCoverWithoutCoverChangesNothing() {
    anaIsLoggedIn();
    tripOf(ana, 1L);

    tripService.deleteCover(1L);

    verify(tripRepository, never()).save(any());
    verifyNoInteractions(imageService);
  }

  @Test
  void deleteCoverOfAnotherUsersTripIsForbidden() {
    anaIsLoggedIn();
    tripOf(bob, 1L);

    assertThatThrownBy(() -> tripService.deleteCover(1L)).isInstanceOf(ForbiddenException.class);
  }

  @Test
  void theCreatorCanReadTheCoverBytes() {
    anaIsLoggedIn();
    Trip trip = tripOf(ana, 1L);
    Image cover = image();
    trip.setCoverImage(cover);
    ImageContentDTO content = new ImageContentDTO(cover.getData(), "image/png");
    when(imageService.findContent(cover.getId())).thenReturn(content);

    assertThat(tripService.findCoverContent(1L)).isEqualTo(content);
  }

  @Test
  void readingTheCoverOfATripWithoutOneIsNotFound() {
    anaIsLoggedIn();
    tripOf(ana, 1L);

    assertThatThrownBy(() -> tripService.findCoverContent(1L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("The trip has no cover");
  }

  @Test
  void readingTheCoverOfAnotherUsersTripIsForbidden() {
    anaIsLoggedIn();
    tripOf(bob, 1L);

    assertThatThrownBy(() -> tripService.findCoverContent(1L))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void readingTheCoverOfAMissingTripIsNotFound() {
    when(tripRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tripService.findCoverContent(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Trip not found");
  }
}
