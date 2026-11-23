package com.ruben.backend.activity.service;

import com.ruben.backend.activity.entity.Activity;
import com.ruben.backend.activity.entity.ActivityLogAction;
import com.ruben.backend.activity.entity.ActivityLogEntry;
import com.ruben.backend.activity.repository.ActivityLogEntryRepository;
import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AuthService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The trip's change log (I4). It only writes what {@link ActivityService} tells it, inside the
 * transaction of the change itself (it never opens its own), so a change that fails leaves no line.
 * Who may change the itinerary has already been decided by the caller.
 */
@Service
public class ActivityLogService {

  private final ActivityLogEntryRepository entryRepository;
  private final UserRepository userRepository;
  private final AuthService authService;

  public ActivityLogService(
      ActivityLogEntryRepository entryRepository,
      UserRepository userRepository,
      AuthService authService) {
    this.entryRepository = entryRepository;
    this.userRepository = userRepository;
    this.authService = authService;
  }

  /** The current user added the activity. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordCreated(Trip trip, Activity activity) {
    record(trip, activity, activity.getName(), ActivityLogAction.CREATED, List.of());
  }

  /** The current user edited the activity; `changedFields` are the names of what changed. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordUpdated(Trip trip, Activity activity, List<String> changedFields) {
    record(trip, activity, activity.getName(), ActivityLogAction.UPDATED, changedFields);
  }

  /** The current user deleted an activity that was called `activityName`; it no longer exists. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordDeleted(Trip trip, String activityName) {
    record(trip, null, activityName, ActivityLogAction.DELETED, List.of());
  }

  /**
   * Makes the log forget an activity that is about to be deleted, keeping its lines. It must run
   * before the activity is deleted, because the lines point at it.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void detachActivity(Long activityId) {
    entryRepository.detachActivity(activityId);
  }

  /**
   * Deletes the whole log of a trip. For the trip's own deletion: the caller has already checked
   * that the user may delete the trip, and calls this before deleting the trip's activities and the
   * trip, because the lines point at both.
   */
  @Transactional
  public void deleteAllByTrip(Long tripId) {
    entryRepository.deleteByTripId(tripId);
  }

  private void record(
      Trip trip,
      Activity activity,
      String activityName,
      ActivityLogAction action,
      List<String> changedFields) {
    UserDTO current = authService.getCurrentUser();
    entryRepository.save(
        new ActivityLogEntry(
            trip,
            activity,
            activityName,
            userRepository.getReferenceById(current.id()),
            current.name(),
            action,
            changedFields.isEmpty() ? null : String.join(",", changedFields)));
  }
}
