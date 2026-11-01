package com.ruben.backend.trip.repository;

import com.ruben.backend.trip.entity.Trip;
import com.ruben.backend.user.entity.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, Long> {

  /** One page of the trips a user created; sorting and paging happen in the query. */
  Page<Trip> findByCreatorId(Long creatorId, Pageable pageable);

  /** One page of the trips whose creator has that role; the example trips are the admin's. */
  Page<Trip> findByCreatorRole(Role role, Pageable pageable);

  /**
   * One page of every trip, whatever its creator, for moderation; `search` matches the name, the
   * destination or the creator's email, ignoring case, and null matches all. The creator is fetched
   * in the same query.
   */
  @Query(
      value =
          "select t from Trip t join fetch t.creator c where (:search is null"
              + " or lower(t.name) like lower(concat('%', :search, '%'))"
              + " or lower(t.destination) like lower(concat('%', :search, '%'))"
              + " or lower(c.email) like lower(concat('%', :search, '%')))",
      countQuery =
          "select count(t) from Trip t join t.creator c where (:search is null"
              + " or lower(t.name) like lower(concat('%', :search, '%'))"
              + " or lower(t.destination) like lower(concat('%', :search, '%'))"
              + " or lower(c.email) like lower(concat('%', :search, '%')))")
  Page<Trip> searchAll(@Param("search") String search, Pageable pageable);

  Optional<Trip> findByIdAndCreatorRole(Long id, Role role);

  List<Trip> findByCreatorIdAndNameIn(Long creatorId, Collection<String> names);
}
