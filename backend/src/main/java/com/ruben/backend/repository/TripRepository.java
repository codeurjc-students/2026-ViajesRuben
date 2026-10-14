package com.ruben.backend.repository;

import com.ruben.backend.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {}
