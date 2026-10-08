package com.ruben.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ruben.backend.dto.TripDTO;
import com.ruben.backend.repository.TripRepository;

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
