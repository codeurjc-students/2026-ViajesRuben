package com.ruben.backend.trip.service;

import com.ruben.backend.activity.dto.FirstActivityDTO;
import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.trip.dto.DayPreviewDTO;
import com.ruben.backend.trip.dto.PublicTripDTO;
import com.ruben.backend.trip.dto.PublicTripSummaryDTO;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The public itineraries (B1, I7): the published trips of any user, read-only and without login.
 * Their creators publish and unpublish them through {@link TripService}.
 */
@Service
public class PublicTripService {

  /** What a client may sort the public list by: the trip properties and the publication date. */
  static final Set<String> SORTABLE = sortable();

  private final TripRepository tripRepository;
  private final TripAccessService tripAccessService;
  private final ActivityService activityService;

  public PublicTripService(
      TripRepository tripRepository,
      TripAccessService tripAccessService,
      ActivityService activityService) {
    this.tripRepository = tripRepository;
    this.tripAccessService = tripAccessService;
    this.activityService = activityService;
  }

  /**
   * One page of public trips, each with the first activity of its first three days; the previews of
   * the whole page come from one query. `search` keeps the trips whose name or destination contains
   * it, ignoring case; null or blank lists all.
   *
   * @throws BadRequestException the page asks to sort by a property that is not sortable
   */
  @Transactional(readOnly = true)
  public PageDTO<PublicTripSummaryDTO> findPage(String search, Pageable pageable) {
    pageable
        .getSort()
        .forEach(
            order -> {
              if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty() + "'");
              }
            });
    String text = search == null || search.isBlank() ? null : search.trim();
    Page<Trip> trips = tripRepository.searchPublished(text, pageable);
    Map<Long, List<FirstActivityDTO>> firstActivities =
        activityService.findFirstActivities(trips.stream().map(Trip::getId).toList()).stream()
            .collect(Collectors.groupingBy(FirstActivityDTO::tripId));
    return PageDTO.from(
        trips.map(
            trip ->
                new PublicTripSummaryDTO(
                    PublicTripDTO.from(trip),
                    firstActivities.getOrDefault(trip.getId(), List.of()).stream()
                        .map(first -> preview(trip, first))
                        .toList())));
  }

  /**
   * @throws ResourceNotFoundException no published trip has that id
   */
  @Transactional(readOnly = true)
  public PublicTripDTO findById(Long id) {
    return PublicTripDTO.from(tripAccessService.findPublished(id));
  }

  private static DayPreviewDTO preview(Trip trip, FirstActivityDTO first) {
    int day = (int) ChronoUnit.DAYS.between(trip.getStartDate(), first.date()) + 1;
    return new DayPreviewDTO(day, first.name());
  }

  private static Set<String> sortable() {
    Set<String> properties = new HashSet<>(TripService.SORTABLE);
    properties.add("publishedAt");
    return Set.copyOf(properties);
  }
}
