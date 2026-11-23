package com.ruben.backend.activity.dto;

import com.ruben.backend.activity.entity.ActivityLogAction;
import com.ruben.backend.activity.entity.ActivityLogEntry;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * One line of a trip's change log (I4). `activityId` is null once the activity was deleted;
 * `activityName` and `userName` are the names at the time of the change. `changedFields` names what
 * an edit changed (never the values) and is empty for any other action.
 */
public record ActivityLogEntryDTO(
    Long id,
    Long activityId,
    String activityName,
    Long userId,
    String userName,
    ActivityLogAction action,
    List<String> changedFields,
    Instant createdAt) {

  public static ActivityLogEntryDTO from(ActivityLogEntry entry) {
    return new ActivityLogEntryDTO(
        entry.getId(),
        entry.getActivity() == null ? null : entry.getActivity().getId(),
        entry.getActivityName(),
        entry.getUser().getId(),
        entry.getUserName(),
        entry.getAction(),
        entry.getChangedFields() == null
            ? List.of()
            : Arrays.asList(entry.getChangedFields().split(",")),
        entry.getCreatedAt());
  }
}
