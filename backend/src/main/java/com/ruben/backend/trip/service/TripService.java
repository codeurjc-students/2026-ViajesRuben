package com.ruben.backend.trip.service;

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
}
