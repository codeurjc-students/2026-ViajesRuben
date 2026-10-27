package com.ruben.backend.activity.repository;

import com.ruben.backend.activity.dto.OutsideDatesDTO;
import com.ruben.backend.activity.dto.OverlapDTO;
import com.ruben.backend.activity.entity.Activity;
import java.math.BigDecimal;
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

  /**
   * The pairs of activities of the same day whose times intersect. Ranges that only touch (one ends
   * when the other starts) do not intersect; activities without both times are ignored.
   */
  @Query(
      "select new com.ruben.backend.activity.dto.OverlapDTO(a.date, a.id, a.name, a.startTime,"
          + " a.endTime, b.id, b.name, b.startTime, b.endTime)"
          + " from Activity a, Activity b"
          + " where a.trip.id = :tripId and b.trip.id = :tripId and a.id < b.id"
          + " and a.date = b.date"
          + " and a.startTime is not null and a.endTime is not null"
          + " and b.startTime is not null and b.endTime is not null"
          + " and a.startTime < b.endTime and b.startTime < a.endTime"
          + " order by a.date, a.startTime, b.startTime")
  List<OverlapDTO> findOverlaps(@Param("tripId") Long tripId);

  /** The activities on a day before the trip starts or after it ends. */
  @Query(
      "select new com.ruben.backend.activity.dto.OutsideDatesDTO(a.id, a.name, a.date)"
          + " from Activity a join a.trip t where t.id = :tripId"
          + " and (a.date < t.startDate or a.date > t.endDate)"
          + " order by a.date, a.startTime, a.id")
  List<OutsideDatesDTO> findOutsideTrip(@Param("tripId") Long tripId);

  /** The sum of the costs of the trip's activities; null when none has a cost. */
  @Query("select sum(a.cost) from Activity a where a.trip.id = :tripId")
  BigDecimal sumCost(@Param("tripId") Long tripId);

  /** The pictures of a trip's activities, read without loading any entity or bytes. */
  @Query("select a.image.id from Activity a where a.trip.id = :tripId and a.image is not null")
  List<UUID> findImageIdsByTripId(@Param("tripId") Long tripId);

  /** Deletes a trip's activities without loading them. */
  @Modifying(flushAutomatically = true)
  @Query("delete from Activity a where a.trip.id = :tripId")
  int deleteByTripId(@Param("tripId") Long tripId);
}
