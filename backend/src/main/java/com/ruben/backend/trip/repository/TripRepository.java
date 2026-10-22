package com.ruben.backend.trip.repository;

import com.ruben.backend.trip.entity.Trip;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {

  List<Trip> findByNameIn(Collection<String> names);
}
