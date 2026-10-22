package com.ruben.backend.trip.service;

import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.repository.TripRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TripService {

  private final TripRepository tripRepository;

  public TripService(TripRepository tripRepository) {
    this.tripRepository = tripRepository;
  }

  public List<TripDTO> findAll() {
    return tripRepository.findAll().stream().map(TripDTO::from).toList();
  }

  public TripDTO findById(Long id) {
    return tripRepository
        .findById(id)
        .map(TripDTO::from)
        .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
  }
}
