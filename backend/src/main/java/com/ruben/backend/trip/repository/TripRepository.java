package com.ruben.backend.trip.repository;

import com.ruben.backend.trip.entity.Trip;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, Long> {

  /** One page of the trips a user created; sorting and paging happen in the query. */
  Page<Trip> findByCreatorId(Long creatorId, Pageable pageable);

  /**
   * One page of the published trips of every user (the public itineraries); `search` matches the
   * name or the destination, ignoring case, and null matches all. The creator is fetched in the
   * same query, because the public list shows the author's name.
   */
  @Query(
      value =
          "select t from Trip t join fetch t.creator where t.published = true and (:search is null"
              + " or lower(t.name) like lower(concat('%', :search, '%'))"
              + " or lower(t.destination) like lower(concat('%', :search, '%')))",
      countQuery =
          "select count(t) from Trip t where t.published = true and (:search is null"
              + " or lower(t.name) like lower(concat('%', :search, '%'))"
              + " or lower(t.destination) like lower(concat('%', :search, '%')))")
  Page<Trip> searchPublished(@Param("search") String search, Pageable pageable);

  /**
   * A published trip with its creator, or empty when the id is unknown or the trip is not public.
   */
  @EntityGraph(attributePaths = "creator")
  Optional<Trip> findByIdAndPublishedTrue(Long id);

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

  /**
   * Makes the copies of a trip independent of it (I9) before it is deleted: their `copiedFrom`
   * becomes null. An explicit update, because the database has no cascade.
   */
  @Modifying(flushAutomatically = true)
  @Query("update Trip t set t.copiedFrom = null where t.copiedFrom.id = :id")
  int clearCopiedFrom(@Param("id") Long id);

  List<Trip> findByCreatorIdAndNameIn(Long creatorId, Collection<String> names);
}
