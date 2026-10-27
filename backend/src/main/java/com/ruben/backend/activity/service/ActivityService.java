package com.ruben.backend.activity.service;

import com.ruben.backend.activity.dto.ActivityDTO;
import com.ruben.backend.activity.dto.ActivityRequest;
import com.ruben.backend.activity.dto.TripCostDTO;
import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.repository.ActivityRepository;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.dto.ImageResult;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.image.service.ImageService;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.service.TripAccessService;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AuthService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * The activities of a trip. Who may see or change them is decided by the trip, through {@link
 * TripAccessService}; an activity id of another trip is not found.
 */
@Service
public class ActivityService {

  private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

  /** The only properties a client may sort by. */
  private static final Set<String> SORTABLE =
      Set.of("id", "date", "startTime", "name", "cost", "createdAt");

  private final ActivityRepository activityRepository;
  private final UserRepository userRepository;
  private final TripAccessService tripAccessService;
  private final AuthService authService;
  private final ImageService imageService;

  public ActivityService(
      ActivityRepository activityRepository,
      UserRepository userRepository,
      TripAccessService tripAccessService,
      AuthService authService,
      ImageService imageService) {
    this.activityRepository = activityRepository;
    this.userRepository = userRepository;
    this.tripAccessService = tripAccessService;
    this.authService = authService;
    this.imageService = imageService;
  }

  /**
   * One page of the trip's activities, only those of `date` when given.
   *
   * @throws BadRequestException the page asks to sort by a property that is not sortable
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user may not see the trip
   */
  @Transactional(readOnly = true)
  public PageDTO<ActivityDTO> findPage(Long tripId, LocalDate date, Pageable pageable) {
    pageable
        .getSort()
        .forEach(
            order -> {
              if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty() + "'");
              }
            });
    tripAccessService.findForRead(tripId);
    return PageDTO.from(
        activityRepository.findByTrip(tripId, date, pageable).map(ActivityDTO::from));
  }

  /**
   * The total cost of the trip's activities and what is left of the budget.
   *
   * @throws ResourceNotFoundException no trip has that id
   * @throws ForbiddenException the user may not see the trip
   */
  @Transactional(readOnly = true)
  public TripCostDTO findCost(Long tripId) {
    Trip trip = tripAccessService.findForRead(tripId);
    BigDecimal total = activityRepository.sumCost(tripId);
    if (total == null) {
      total = BigDecimal.ZERO;
    }
    BigDecimal budget = trip.getBudget();
    return new TripCostDTO(total, budget == null ? null : budget.subtract(total));
  }

  /**
   * The current user becomes the author.
   *
   * @throws BadRequestException the date is outside the trip's dates
   */
  @Transactional
  public ActivityDTO create(Long tripId, ActivityRequest request) {
    Trip trip = tripAccessService.findForWrite(tripId);
    checkDateInsideTrip(trip, request);
    Activity activity =
        new Activity(
            trip,
            userRepository.getReferenceById(authService.getCurrentUser().id()),
            request.name().trim(),
            request.date());
    applyFields(activity, request);
    activity = activityRepository.saveAndFlush(activity);
    log.info("Activity {} created in trip {}", activity.getId(), tripId);
    return ActivityDTO.from(activity);
  }

  /**
   * @throws ResourceNotFoundException no such trip, or the activity is not of that trip
   */
  @Transactional(readOnly = true)
  public ActivityDTO findById(Long tripId, Long id) {
    tripAccessService.findForRead(tripId);
    return ActivityDTO.from(findActivity(tripId, id));
  }

  /** Replaces every editable field; the picture is not touched. */
  @Transactional
  public ActivityDTO update(Long tripId, Long id, ActivityRequest request) {
    Trip trip = tripAccessService.findForWrite(tripId);
    Activity activity = findActivity(tripId, id);
    checkDateInsideTrip(trip, request);
    activity.setName(request.name().trim());
    activity.setDate(request.date());
    applyFields(activity, request);
    activity = activityRepository.saveAndFlush(activity);
    log.info("Activity {} of trip {} updated", id, tripId);
    return ActivityDTO.from(activity);
  }

  /** Deletes the activity and then its picture, in this order because the activity points at it. */
  @Transactional
  public void delete(Long tripId, Long id) {
    tripAccessService.findForWrite(tripId);
    Activity activity = findActivity(tripId, id);
    Image image = activity.getImage();
    activityRepository.delete(activity);
    if (image != null) {
      imageService.delete(image.getId());
    }
    log.info("Activity {} of trip {} deleted", id, tripId);
  }

  /** Stores the picture and deletes the previous one, so no image is left behind. */
  @Transactional
  public ImageResult replaceImage(Long tripId, Long id, MultipartFile file) {
    tripAccessService.findForWrite(tripId);
    Activity activity = findActivity(tripId, id);
    Image previous = activity.getImage();
    Image image = imageService.save(file);
    activity.setImage(image);
    activityRepository.save(activity);
    if (previous != null) {
      imageService.delete(previous.getId());
    }
    log.info("Activity {} {} its image", id, previous == null ? "got" : "replaced");
    return new ImageResult(ImageDTO.from(image), previous == null);
  }

  /** Removes the picture; doing it when there is none changes nothing. */
  @Transactional
  public void deleteImage(Long tripId, Long id) {
    tripAccessService.findForWrite(tripId);
    Activity activity = findActivity(tripId, id);
    Image image = activity.getImage();
    if (image == null) {
      return;
    }
    activity.setImage(null);
    activityRepository.save(activity);
    imageService.delete(image.getId());
    log.info("Activity {} lost its image", id);
  }

  /**
   * @throws ResourceNotFoundException no such trip or activity, or the activity has no picture
   */
  @Transactional(readOnly = true)
  public ImageContentDTO findImageContent(Long tripId, Long id) {
    tripAccessService.findForRead(tripId);
    Image image = findActivity(tripId, id).getImage();
    if (image == null) {
      throw new ResourceNotFoundException("The activity has no image");
    }
    return imageService.findContent(image.getId());
  }

  /**
   * Deletes every activity of a trip and their pictures. For the trip's own deletion: the caller
   * has already checked that the user may delete the trip.
   */
  @Transactional
  public void deleteAllByTrip(Long tripId) {
    List<UUID> imageIds = activityRepository.findImageIdsByTripId(tripId);
    activityRepository.deleteByTripId(tripId);
    imageService.deleteAll(imageIds);
  }

  private Activity findActivity(Long tripId, Long id) {
    return activityRepository
        .findByIdAndTripId(id, tripId)
        .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
  }

  private static void checkDateInsideTrip(Trip trip, ActivityRequest request) {
    if (request.date().isBefore(trip.getStartDate()) || request.date().isAfter(trip.getEndDate())) {
      throw new BadRequestException("The date is outside the trip's dates");
    }
  }

  private static void applyFields(Activity activity, ActivityRequest request) {
    activity.setStartTime(request.startTime());
    activity.setEndTime(request.endTime());
    activity.setCost(request.cost());
    activity.setCategory(request.category());
    activity.setLocation(blankToNull(request.location()));
    activity.setDescription(blankToNull(request.description()));
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.trim();
  }
}
