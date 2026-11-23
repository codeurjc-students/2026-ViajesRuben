package com.ruben.backend.activity.repository;

import com.ruben.backend.activity.entity.ActivityLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogEntryRepository extends JpaRepository<ActivityLogEntry, Long> {

  /** One page of a trip's log; sorting and paging happen in the query. */
  Page<ActivityLogEntry> findByTripId(Long tripId, Pageable pageable);

  /**
   * Makes the entries of an activity forget it, without loading them: done before the activity is
   * deleted, because they point at it.
   */
  @Modifying(flushAutomatically = true)
  @Query("update ActivityLogEntry e set e.activity = null where e.activity.id = :activityId")
  int detachActivity(@Param("activityId") Long activityId);

  /** Deletes a trip's log without loading it. */
  @Modifying(flushAutomatically = true)
  @Query("delete from ActivityLogEntry e where e.trip.id = :tripId")
  int deleteByTripId(@Param("tripId") Long tripId);
}
