package com.ruben.backend.activity.service;

import com.ruben.backend.activity.dto.ActivityRequest;
import com.ruben.backend.activity.entity.Activity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Which editable fields of an activity an edit really changes, for the change log (I4). Pure: it
 * compares the activity as it is with the request as it will be stored (trimmed text, blank as
 * null), and never reads or writes anything else. Only names are returned, never values.
 */
final class ActivityFieldChanges {

  /** The name of the log entry's field for a change of the picture. */
  static final String IMAGE = "image";

  private ActivityFieldChanges() {}

  /** The names of the fields that differ, in the order of the activity form. */
  static List<String> between(Activity activity, ActivityRequest request) {
    List<String> changed = new ArrayList<>();
    addIf(changed, "name", !Objects.equals(activity.getName(), request.name().trim()));
    addIf(
        changed,
        "description",
        !Objects.equals(activity.getDescription(), blankToNull(request.description())));
    addIf(
        changed,
        "location",
        !Objects.equals(activity.getLocation(), blankToNull(request.location())));
    addIf(changed, "date", !Objects.equals(activity.getDate(), request.date()));
    addIf(changed, "startTime", !Objects.equals(activity.getStartTime(), request.startTime()));
    addIf(changed, "endTime", !Objects.equals(activity.getEndTime(), request.endTime()));
    addIf(changed, "cost", differentCost(activity.getCost(), request.cost()));
    addIf(changed, "category", activity.getCategory() != request.category());
    return changed;
  }

  /** Costs are equal when their amounts are, whatever their scale (16.5 and 16.50). */
  private static boolean differentCost(BigDecimal current, BigDecimal requested) {
    if (current == null || requested == null) {
      return current != requested;
    }
    return current.compareTo(requested) != 0;
  }

  private static void addIf(List<String> changed, String field, boolean differs) {
    if (differs) {
      changed.add(field);
    }
  }

  /** The same normalisation as when the activity is stored. */
  static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.trim();
  }
}
