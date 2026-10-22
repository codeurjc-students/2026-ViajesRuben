package com.ruben.backend.trip.service;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.trip.dto.TripDTO;
import com.ruben.backend.trip.repository.TripRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TripService {

  private final TripRepository tripRepository;

  public TripService(TripRepository tripRepository) {
    this.tripRepository = tripRepository;
  }

  public PageDTO<TripDTO> findPage(Pageable pageable) {
    return PageDTO.from(tripRepository.findAll(pageable).map(TripDTO::from));
  }

  public TripDTO findById(Long id) {
    return tripRepository
        .findById(id)
        .map(TripDTO::from)
        .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
  }
}
