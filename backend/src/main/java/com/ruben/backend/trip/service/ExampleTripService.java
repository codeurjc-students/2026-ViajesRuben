package com.ruben.backend.trip.service;

import com.ruben.backend.activity.dto.FirstActivityDTO;
import com.ruben.backend.activity.service.ActivityService;
import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.trip.dto.DayPreviewDTO;
import com.ruben.backend.trip.dto.ExampleTripDTO;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.entity.Role;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The example trips of the public home page (B1): the trips the administrator created, read-only
 * and without login. The administrator manages them like any other trip.
 */
@Service
public class ExampleTripService {

  private final TripRepository tripRepository;
  private final TripAccessService tripAccessService;
  private final ActivityService activityService;

  public ExampleTripService(
      TripRepository tripRepository,
      TripAccessService tripAccessService,
      ActivityService activityService) {
    this.tripRepository = tripRepository;
    this.tripAccessService = tripAccessService;
    this.activityService = activityService;
  }

  /**
   * One page of example trips, each with the first activity of its first three days; the previews
   * of the whole page come from one query.
   *
   * @throws BadRequestException the page asks to sort by a property that is not sortable
   */
  @Transactional(readOnly = true)
  public PageDTO<ExampleTripDTO> findPage(Pageable pageable) {
    pageable
        .getSort()
        .forEach(
            order -> {
              if (!TripService.SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty() + "'");
              }
            });
    Page<Trip> trips = tripRepository.findByCreatorRole(Role.ADMIN, pageable);
    Map<Long, List<FirstActivityDTO>> firstActivities =
        activityService.findFirstActivities(trips.stream().map(Trip::getId).toList()).stream()
            .collect(Collectors.groupingBy(FirstActivityDTO::tripId));
    return PageDTO.from(
        trips.map(
            trip ->
                new ExampleTripDTO(
                    TripDTO.from(trip),
                    firstActivities.getOrDefault(trip.getId(), List.of()).stream()
                        .map(first -> preview(trip, first))
                        .toList())));
  }

  /**
   * @throws ResourceNotFoundException no example trip has that id
   */
  @Transactional(readOnly = true)
  public TripDTO findById(Long id) {
    return TripDTO.from(tripAccessService.findExample(id));
  }

  private static DayPreviewDTO preview(Trip trip, FirstActivityDTO first) {
    int day = (int) ChronoUnit.DAYS.between(trip.getStartDate(), first.date()) + 1;
    return new DayPreviewDTO(day, first.name());
  }
}
