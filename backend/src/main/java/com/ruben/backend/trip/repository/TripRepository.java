package com.ruben.backend.trip.repository;

import com.ruben.backend.trip.entity.Trip;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {

  /** One page of the trips a user created; sorting and paging happen in the query. */
  Page<Trip> findByCreatorId(Long creatorId, Pageable pageable);

  List<Trip> findByCreatorIdAndNameIn(Long creatorId, Collection<String> names);
}
