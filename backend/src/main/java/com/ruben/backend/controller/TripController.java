package com.ruben.backend.controller;

import com.ruben.backend.dto.TripDTO;
import com.ruben.backend.service.TripService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
public class TripController {

  private final TripService tripService;

  public TripController(TripService tripService) {
    this.tripService = tripService;
  }

  @GetMapping
  public List<TripDTO> getTrips() {
    return tripService.findAll();
  }
}
