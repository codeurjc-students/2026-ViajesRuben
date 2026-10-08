package com.ruben.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ruben.backend.model.Trip;

public interface TripRepository extends JpaRepository<Trip, Long> {
}
