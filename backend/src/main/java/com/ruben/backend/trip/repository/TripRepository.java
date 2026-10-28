package com.ruben.backend.trip.repository;

import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.entity.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {

  /** One page of the trips a user created; sorting and paging happen in the query. */
  Page<Trip> findByCreatorId(Long creatorId, Pageable pageable);

  /** One page of the trips whose creator has that role; the example trips are the admin's. */
  Page<Trip> findByCreatorRole(Role role, Pageable pageable);

  Optional<Trip> findByIdAndCreatorRole(Long id, Role role);

  List<Trip> findByCreatorIdAndNameIn(Long creatorId, Collection<String> names);
}
