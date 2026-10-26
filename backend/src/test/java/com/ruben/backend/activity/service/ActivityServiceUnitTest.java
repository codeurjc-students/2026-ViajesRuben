package com.ruben.backend.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.dto.ActivityRequest;
import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.entity.ActivityCategory;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageResult;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.image.service.ImageService;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.service.TripAccessService;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AuthService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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

@Tag("B5")
@ExtendWith(MockitoExtension.class)
class ActivityServiceUnitTest {

  private static final LocalDate START = LocalDate.of(2027, 7, 10);
  private static final LocalDate END = LocalDate.of(2027, 7, 17);
  private static final MockMultipartFile PICTURE =
      new MockMultipartFile("file", "a.png", "image/png", ImageTestData.png(100));

  @Mock private ActivityRepository activityRepository;
  @Mock private UserRepository userRepository;
  @Mock private TripAccessService tripAccessService;
  @Mock private AuthService authService;
  @Mock private ImageService imageService;

  private ActivityService activityService;
  private User ana;
  private Trip trip;

  @BeforeEach
  void setUp() {
    activityService =
        new ActivityService(
            activityRepository, userRepository, tripAccessService, authService, imageService);
    ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    trip = new Trip(ana, "Trip", "Rome", START, END);
    ReflectionTestUtils.setField(trip, "id", 1L);
    lenient().when(tripAccessService.findForRead(1L)).thenReturn(trip);
    lenient().when(tripAccessService.findForWrite(1L)).thenReturn(trip);
  }

  private static ActivityRequest request(String name, LocalDate date) {
    return new ActivityRequest(
        name,
        date,
        LocalTime.of(10, 0),
        LocalTime.of(12, 30),
        "  Piazza ",
        "  Guided tour  ",
        new BigDecimal("16.5"),
        ActivityCategory.SIGHTSEEING);
  }

  private Activity activity(Long id) {
    Activity activity = new Activity(trip, ana, "Colosseum", START.plusDays(1));
    ReflectionTestUtils.setField(activity, "id", id);
    lenient().when(activityRepository.findByIdAndTripId(id, 1L)).thenReturn(Optional.of(activity));
    return activity;
  }

  private static Image image() {
    Image image = new Image(ImageTestData.png(10), "image/png");
    ReflectionTestUtils.setField(image, "id", UUID.randomUUID());
    return image;
  }

  /** Nobody may touch the trip: the access service refuses, whatever the operation. */
  private void tripIsRefused(RuntimeException refusal) {
    lenient().when(tripAccessService.findForRead(1L)).thenThrow(refusal);
    lenient().when(tripAccessService.findForWrite(1L)).thenThrow(refusal);
  }

  // --- list ---

  @Test
  void findPagePassesTheTripTheDayAndThePageToTheQuery() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("date", "startTime", "id"));
    LocalDate day = START.plusDays(1);
    List<Activity> found = List.of(activity(5L), activity(6L));
    when(activityRepository.findByTrip(1L, day, pageable))
        .thenReturn(new PageImpl<>(found, pageable, 2));

    PageDTO<ActivityDTO> page = activityService.findPage(1L, day, pageable);

    assertThat(page.content()).extracting(ActivityDTO::id).containsExactly(5L, 6L);
    assertThat(page.content()).extracting(ActivityDTO::tripId).containsOnly(1L);
    assertThat(page.totalElements()).isEqualTo(2);
  }

  @Test
  void findPageWithoutDayAsksForTheWholeItinerary() {
    Pageable pageable = PageRequest.of(0, 10);
    when(activityRepository.findByTrip(1L, null, pageable)).thenReturn(new PageImpl<>(List.of()));

    assertThat(activityService.findPage(1L, null, pageable).content()).isEmpty();
  }

  @Test
  void findPageAcceptsEverySortableProperty() {
    Pageable pageable =
        PageRequest.of(0, 10, Sort.by("id", "date", "startTime", "name", "cost", "createdAt"));
    when(activityRepository.findByTrip(1L, null, pageable)).thenReturn(new PageImpl<>(List.of()));

    assertThat(activityService.findPage(1L, null, pageable).content()).isEmpty();
  }

  @Test
  void findPageRejectsSortingByAnotherPropertyBeforeLookingAtTheTrip() {
    Pageable nested = PageRequest.of(0, 10, Sort.by("trip.creator.password"));

    assertThatThrownBy(() -> activityService.findPage(1L, null, nested))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot sort by 'trip.creator.password'");
    verifyNoInteractions(tripAccessService, activityRepository);
  }

  @Test
  void findPageOfAnotherUsersTripIsRefusedAndQueriesNothing() {
    tripIsRefused(new ForbiddenException("Only the creator can access this trip"));

    assertThatThrownBy(() -> activityService.findPage(1L, null, PageRequest.of(0, 10)))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(activityRepository);
  }

  // --- create ---

  @Test
  void createStoresTheActivityOfTheTripForTheCurrentUserWithCleanText() {
    when(authService.getCurrentUser()).thenReturn(UserDTO.from(ana));
    when(userRepository.getReferenceById(7L)).thenReturn(ana);
    when(activityRepository.saveAndFlush(any(Activity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ActivityDTO created = activityService.create(1L, request("  Colosseum ", START.plusDays(1)));

    assertThat(created.tripId()).isEqualTo(1L);
    assertThat(created.authorId()).isEqualTo(7L);
    assertThat(created.name()).isEqualTo("Colosseum");
    assertThat(created.location()).isEqualTo("Piazza");
    assertThat(created.description()).isEqualTo("Guided tour");
    assertThat(created.date()).isEqualTo(START.plusDays(1));
    assertThat(created.startTime()).isEqualTo(LocalTime.of(10, 0));
    assertThat(created.endTime()).isEqualTo(LocalTime.of(12, 30));
    assertThat(created.cost()).isEqualByComparingTo("16.50");
    assertThat(created.category()).isEqualTo(ActivityCategory.SIGHTSEEING);
    assertThat(created.imageId()).isNull();
  }

  @Test
  void createTurnsBlankOptionalTextIntoNull() {
    when(authService.getCurrentUser()).thenReturn(UserDTO.from(ana));
    when(userRepository.getReferenceById(7L)).thenReturn(ana);
    when(activityRepository.saveAndFlush(any(Activity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ActivityDTO created =
        activityService.create(
            1L, new ActivityRequest("Walk", START, null, null, "  ", "", null, null));

    assertThat(created.location()).isNull();
    assertThat(created.description()).isNull();
    assertThat(created.startTime()).isNull();
    assertThat(created.cost()).isNull();
  }

  @Test
  void createAcceptsTheFirstAndTheLastDayOfTheTrip() {
    when(authService.getCurrentUser()).thenReturn(UserDTO.from(ana));
    when(userRepository.getReferenceById(7L)).thenReturn(ana);
    when(activityRepository.saveAndFlush(any(Activity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    assertThat(activityService.create(1L, request("First", START)).date()).isEqualTo(START);
    assertThat(activityService.create(1L, request("Last", END)).date()).isEqualTo(END);
  }

  @Test
  void createRejectsADateBeforeOrAfterTheTrip() {
    assertThatThrownBy(() -> activityService.create(1L, request("Early", START.minusDays(1))))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("The date is outside the trip's dates");
    assertThatThrownBy(() -> activityService.create(1L, request("Late", END.plusDays(1))))
        .isInstanceOf(BadRequestException.class);
    verify(activityRepository, never()).saveAndFlush(any());
  }

  @Test
  void createInAnotherUsersTripIsRefusedAndWritesNothing() {
    tripIsRefused(new ForbiddenException("Only the creator can access this trip"));

    assertThatThrownBy(() -> activityService.create(1L, request("Walk", START)))
        .isInstanceOf(ForbiddenException.class);
    verify(activityRepository, never()).saveAndFlush(any());
  }

  // --- read ---

  @Test
  void findByIdReturnsTheActivityOfTheTrip() {
    activity(5L);

    assertThat(activityService.findById(1L, 5L).name()).isEqualTo("Colosseum");
  }

  @Test
  void findByIdOfAnActivityOfAnotherTripIsNotFound() {
    when(activityRepository.findByIdAndTripId(5L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> activityService.findById(1L, 5L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Activity not found");
  }

  @Test
  void findByIdOfAMissingOrForeignTripIsRefusedBeforeLookingForTheActivity() {
    tripIsRefused(new ResourceNotFoundException("Trip not found"));

    assertThatThrownBy(() -> activityService.findById(1L, 5L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Trip not found");
    verifyNoInteractions(activityRepository);
  }

  // --- update ---

  @Test
  void updateReplacesEveryEditableFieldAndKeepsTheImage() {
    Activity activity = activity(5L);
    Image image = image();
    activity.setImage(image);
    when(activityRepository.saveAndFlush(activity)).thenReturn(activity);

    ActivityDTO updated = activityService.update(1L, 5L, request(" Forum ", END));

    assertThat(updated.name()).isEqualTo("Forum");
    assertThat(updated.date()).isEqualTo(END);
    assertThat(updated.startTime()).isEqualTo(LocalTime.of(10, 0));
    assertThat(updated.cost()).isEqualByComparingTo("16.50");
    assertThat(updated.location()).isEqualTo("Piazza");
    assertThat(updated.imageId()).isEqualTo(image.getId());
  }

  @Test
  void updateClearsTheOptionalFieldsThatAreMissingFromTheRequest() {
    Activity activity = activity(5L);
    activity.setCost(new BigDecimal("3.00"));
    activity.setStartTime(LocalTime.of(8, 0));
    when(activityRepository.saveAndFlush(activity)).thenReturn(activity);

    ActivityDTO updated =
        activityService.update(
            1L, 5L, new ActivityRequest("Walk", START, null, null, null, null, null, null));

    assertThat(updated.cost()).isNull();
    assertThat(updated.startTime()).isNull();
    assertThat(updated.category()).isNull();
  }

  @Test
  void updateRejectsADateOutsideTheTripAndChangesNothing() {
    Activity activity = activity(5L);

    assertThatThrownBy(() -> activityService.update(1L, 5L, request("Late", END.plusDays(1))))
        .isInstanceOf(BadRequestException.class);
    assertThat(activity.getName()).isEqualTo("Colosseum");
    verify(activityRepository, never()).saveAndFlush(any());
  }

  @Test
  void updateOfAnActivityOfAnotherTripIsNotFound() {
    when(activityRepository.findByIdAndTripId(5L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> activityService.update(1L, 5L, request("Walk", START)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void updateInAnotherUsersTripIsRefusedBeforeLookingForTheActivity() {
    tripIsRefused(new ForbiddenException("Only the creator can access this trip"));

    assertThatThrownBy(() -> activityService.update(1L, 5L, request("Walk", START)))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(activityRepository);
  }

  // --- delete ---

  @Test
  void deleteRemovesTheActivityAndThenItsImage() {
    Activity activity = activity(5L);
    Image image = image();
    activity.setImage(image);

    activityService.delete(1L, 5L);

    InOrder order = Mockito.inOrder(activityRepository, imageService);
    order.verify(activityRepository).delete(activity);
    order.verify(imageService).delete(image.getId());
  }

  @Test
  void deleteOfAnActivityWithoutImageDoesNotTouchImages() {
    Activity activity = activity(5L);

    activityService.delete(1L, 5L);

    verify(activityRepository).delete(activity);
    verifyNoInteractions(imageService);
  }

  @Test
  void deleteOfAnActivityOfAnotherTripIsNotFoundAndDeletesNothing() {
    when(activityRepository.findByIdAndTripId(5L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> activityService.delete(1L, 5L))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(activityRepository, never()).delete(any());
    verifyNoInteractions(imageService);
  }

  @Test
  void deleteInAnotherUsersTripIsRefusedAndDeletesNothing() {
    tripIsRefused(new ForbiddenException("Only the creator can access this trip"));

    assertThatThrownBy(() -> activityService.delete(1L, 5L)).isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(activityRepository, imageService);
  }

  // --- image ---

  @Test
  void theFirstImageIsStoredAndReportedAsCreated() {
    Activity activity = activity(5L);
    Image stored = image();
    when(imageService.save(PICTURE)).thenReturn(stored);

    ImageResult result = activityService.replaceImage(1L, 5L, PICTURE);

    assertThat(result.created()).isTrue();
    assertThat(result.image().id()).isEqualTo(stored.getId());
    assertThat(activity.getImage()).isSameAs(stored);
    verify(activityRepository).save(activity);
    verify(imageService, never()).delete(any());
  }

  @Test
  void replacingTheImageDeletesThePreviousOne() {
    Activity activity = activity(5L);
    Image previous = image();
    activity.setImage(previous);
    Image stored = image();
    when(imageService.save(PICTURE)).thenReturn(stored);

    ImageResult result = activityService.replaceImage(1L, 5L, PICTURE);

    assertThat(result.created()).isFalse();
    assertThat(activity.getImage()).isSameAs(stored);
    verify(imageService).delete(previous.getId());
  }

  @Test
  void aRejectedImageLeavesTheActivityUntouched() {
    Activity activity = activity(5L);
    when(imageService.save(PICTURE)).thenThrow(new BadRequestException("The image is empty"));

    assertThatThrownBy(() -> activityService.replaceImage(1L, 5L, PICTURE))
        .isInstanceOf(BadRequestException.class);
    assertThat(activity.getImage()).isNull();
    verify(activityRepository, never()).save(any());
  }

  @Test
  void replacingTheImageOfAnActivityOfAnotherTripIsNotFoundAndStoresNothing() {
    when(activityRepository.findByIdAndTripId(5L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> activityService.replaceImage(1L, 5L, PICTURE))
        .isInstanceOf(ResourceNotFoundException.class);
    verifyNoInteractions(imageService);
  }

  @Test
  void replacingTheImageInAnotherUsersTripIsRefusedAndStoresNothing() {
    tripIsRefused(new ForbiddenException("Only the creator can access this trip"));

    assertThatThrownBy(() -> activityService.replaceImage(1L, 5L, PICTURE))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(imageService, activityRepository);
  }

  @Test
  void deleteImageUnlinksAndDeletesThePicture() {
    Activity activity = activity(5L);
    Image image = image();
    activity.setImage(image);

    activityService.deleteImage(1L, 5L);

    assertThat(activity.getImage()).isNull();
    verify(activityRepository).save(activity);
    verify(imageService).delete(image.getId());
  }

  @Test
  void deleteImageWithoutImageChangesNothing() {
    activity(5L);

    activityService.deleteImage(1L, 5L);

    verify(activityRepository, never()).save(any());
    verifyNoInteractions(imageService);
  }

  @Test
  void deleteImageInAnotherUsersTripIsRefused() {
    tripIsRefused(new ForbiddenException("Only the creator can access this trip"));

    assertThatThrownBy(() -> activityService.deleteImage(1L, 5L))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void theImageBytesAreReadThroughTheTripPermission() {
    Activity activity = activity(5L);
    Image image = image();
    activity.setImage(image);
    ImageContentDTO content = new ImageContentDTO(image.getData(), "image/png");
    when(imageService.findContent(image.getId())).thenReturn(content);

    assertThat(activityService.findImageContent(1L, 5L)).isEqualTo(content);
    verify(tripAccessService).findForRead(1L);
  }

  @Test
  void readingTheImageOfAnActivityWithoutOneIsNotFound() {
    activity(5L);

    assertThatThrownBy(() -> activityService.findImageContent(1L, 5L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("The activity has no image");
  }

  @Test
  void readingTheImageInAnotherUsersTripIsRefused() {
    tripIsRefused(new ForbiddenException("Only the creator can access this trip"));

    assertThatThrownBy(() -> activityService.findImageContent(1L, 5L))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(activityRepository, imageService);
  }

  // --- for the trip's own delete and update ---

  @Test
  void deleteAllByTripDeletesTheActivitiesAndThenTheirImages() {
    List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
    when(activityRepository.findImageIdsByTripId(1L)).thenReturn(ids);

    activityService.deleteAllByTrip(1L);

    InOrder order = Mockito.inOrder(activityRepository, imageService);
    order.verify(activityRepository).findImageIdsByTripId(1L);
    order.verify(activityRepository).deleteByTripId(1L);
    order.verify(imageService).deleteAll(ids);
    verifyNoInteractions(tripAccessService);
  }

  @Test
  void deleteAllByTripWithoutImagesStillDeletesTheActivities() {
    when(activityRepository.findImageIdsByTripId(1L)).thenReturn(List.of());

    activityService.deleteAllByTrip(1L);

    verify(activityRepository).deleteByTripId(1L);
    verify(imageService).deleteAll(List.of());
  }

  @Test
  void existsOutsideAsksTheQueryAndNotThePermission() {
    when(activityRepository.existsOutside(1L, START, END)).thenReturn(true);
    when(activityRepository.existsOutside(1L, START, END.plusDays(1))).thenReturn(false);

    assertThat(activityService.existsOutside(1L, START, END)).isTrue();
    assertThat(activityService.existsOutside(1L, START, END.plusDays(1))).isFalse();
    verifyNoInteractions(tripAccessService);
  }
}
