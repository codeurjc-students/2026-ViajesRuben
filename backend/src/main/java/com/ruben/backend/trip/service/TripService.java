package com.ruben.backend.trip.service;

import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.dto.ImageResult;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.image.service.ImageService;
import com.ruben.backend.trip.dto.AdminTripDTO;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.dto.TripRequest;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AuthService;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TripService {

  private static final Logger log = LoggerFactory.getLogger(TripService.class);

  /** The only properties a client may sort by; a nested path such as `creator.password` is not. */
  static final Set<String> SORTABLE =
      Set.of("id", "name", "destination", "startDate", "endDate", "createdAt");

  private final TripRepository tripRepository;
  private final UserRepository userRepository;
  private final AuthService authService;
  private final ImageService imageService;
  private final TripAccessService tripAccessService;
  private final ActivityService activityService;

  public TripService(
      TripRepository tripRepository,
      UserRepository userRepository,
      AuthService authService,
      ImageService imageService,
      TripAccessService tripAccessService,
      ActivityService activityService) {
    this.tripRepository = tripRepository;
    this.userRepository = userRepository;
    this.authService = authService;
    this.imageService = imageService;
    this.tripAccessService = tripAccessService;
    this.activityService = activityService;
  }

  /**
   * One page of the trips the current user created.
   *
   * @throws BadRequestException the page asks to sort by a property that is not sortable
   */
  public PageDTO<TripDTO> findPage(Pageable pageable) {
    checkSortable(pageable);
    Long userId = authService.getCurrentUser().id();
    return PageDTO.from(tripRepository.findByCreatorId(userId, pageable).map(TripDTO::from));
  }

  /**
   * One page of every trip of every user, for the administrator; `search` matches the name, the
   * destination or the creator's email, ignoring case, and a blank one lists everything.
   *
   * @throws BadRequestException the page asks to sort by a property that is not sortable
   */
  @Transactional(readOnly = true)
  public PageDTO<AdminTripDTO> findAllPage(String search, Pageable pageable) {
    checkSortable(pageable);
    String text = search == null || search.isBlank() ? null : search.trim();
    return PageDTO.from(tripRepository.searchAll(text, pageable).map(AdminTripDTO::from));
  }

  /** The current user becomes the creator. */
  @Transactional
  public TripDTO create(TripRequest request) {
    Trip trip =
        new Trip(
            userRepository.getReferenceById(authService.getCurrentUser().id()),
            request.name().trim(),
            request.destination().trim(),
            request.startDate(),
            request.endDate());
    applyOptionalFields(trip, request);
    trip = tripRepository.saveAndFlush(trip);
    log.info("User {} created trip {}", trip.getCreator().getId(), trip.getId());
    return TripDTO.from(trip);
  }

  /**
   * @throws ResourceNotFoundException no trip has that id
   * @throws com.ruben.backend.common.error.ForbiddenException the trip is not the user's
   */
  @Transactional(readOnly = true)
  public TripDTO findById(Long id) {
    return TripDTO.from(tripAccessService.findForRead(id));
  }

  /**
   * Replaces every editable field; the cover is not touched. Activities left outside the new dates
   * are not an error: they are reported as a conflict.
   */
  @Transactional
  public TripDTO update(Long id, TripRequest request) {
    Trip trip = tripAccessService.findForWrite(id);
    trip.setName(request.name().trim());
    trip.setDestination(request.destination().trim());
    trip.setStartDate(request.startDate());
    trip.setEndDate(request.endDate());
    applyOptionalFields(trip, request);
    trip = tripRepository.saveAndFlush(trip);
    log.info("Trip {} updated", trip.getId());
    return TripDTO.from(trip);
  }

  /**
   * The creator, or the administrator (B8), deletes the trip: its activities (with their pictures),
   * then the trip, then its cover image, in this order because each points at the next. Features
   * that add children of a trip (invitations, participations) delete them here first, explicitly,
   * before the trip.
   */
  @Transactional
  public void delete(Long id) {
    Trip trip = tripAccessService.findForDelete(id);
    Image cover = trip.getCoverImage();
    Long creatorId = trip.getCreator().getId();
    activityService.deleteAllByTrip(id);
    tripRepository.delete(trip);
    if (cover != null) {
      imageService.delete(cover.getId());
    }
    log.info(
        "Trip {} of user {} deleted by user {}", id, creatorId, authService.getCurrentUser().id());
  }

  /** Stores the picture as the cover and deletes the previous one, so no image is left behind. */
  @Transactional
  public ImageResult replaceCover(Long id, MultipartFile file) {
    Trip trip = tripAccessService.findForWrite(id);
    Image previous = trip.getCoverImage();
    Image cover = imageService.save(file);
    trip.setCoverImage(cover);
    tripRepository.save(trip);
    if (previous != null) {
      imageService.delete(previous.getId());
    }
    log.info("Trip {} {} its cover", id, previous == null ? "got" : "replaced");
    return new ImageResult(ImageDTO.from(cover), previous == null);
  }

  /** Removes the cover; doing it when there is none changes nothing. */
  @Transactional
  public void deleteCover(Long id) {
    Trip trip = tripAccessService.findForWrite(id);
    Image cover = trip.getCoverImage();
    if (cover == null) {
      return;
    }
    trip.setCoverImage(null);
    tripRepository.save(trip);
    imageService.delete(cover.getId());
    log.info("Trip {} lost its cover", id);
  }

  /**
   * @throws ResourceNotFoundException no trip has that id, or the trip has no cover
   */
  @Transactional(readOnly = true)
  public ImageContentDTO findCoverContent(Long id) {
    Image cover = tripAccessService.findForRead(id).getCoverImage();
    if (cover == null) {
      throw new ResourceNotFoundException("The trip has no cover");
    }
    return imageService.findContent(cover.getId());
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

  private static void applyOptionalFields(Trip trip, TripRequest request) {
    trip.setBudget(request.budget());
    String description = request.description();
    trip.setDescription(description == null || description.isBlank() ? null : description.trim());
  }
}
