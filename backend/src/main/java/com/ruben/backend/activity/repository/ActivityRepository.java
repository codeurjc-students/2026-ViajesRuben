package com.ruben.backend.activity.repository;

import com.ruben.backend.activity.entity.Activity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

  /**
   * One page of a trip's activities, all of them or only those of one day; filtering, sorting and
   * paging happen in the query.
   */
  @Query("select a from Activity a where a.trip.id = :tripId and (:date is null or a.date = :date)")
  Page<Activity> findByTrip(
      @Param("tripId") Long tripId, @Param("date") LocalDate date, Pageable pageable);

  /** The activity only when it belongs to that trip, so another trip's id is not found. */
  Optional<Activity> findByIdAndTripId(Long id, Long tripId);

  boolean existsByTripId(Long tripId);

  /** True when some activity of the trip is on a day before `start` or after `end`. */
  @Query(
      "select count(a) > 0 from Activity a where a.trip.id = :tripId"
          + " and (a.date < :start or a.date > :end)")
  boolean existsOutside(
      @Param("tripId") Long tripId, @Param("start") LocalDate start, @Param("end") LocalDate end);

  /** The pictures of a trip's activities, read without loading any entity or bytes. */
  @Query("select a.image.id from Activity a where a.trip.id = :tripId and a.image is not null")
  List<UUID> findImageIdsByTripId(@Param("tripId") Long tripId);

  /** Deletes a trip's activities without loading them. */
  @Modifying(flushAutomatically = true)
  @Query("delete from Activity a where a.trip.id = :tripId")
  int deleteByTripId(@Param("tripId") Long tripId);
}
