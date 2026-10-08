package com.ruben.backend;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ruben.backend.model.Trip;
import com.ruben.backend.repository.TripRepository;

@Component
public class DataLoader implements CommandLineRunner {

    private final TripRepository tripRepository;

    public DataLoader(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Override
    public void run(String... args) {
        if (tripRepository.count() == 0) {
            tripRepository.saveAll(List.of(
                    new Trip("Trip 1", "Lisbon", LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 5)),
                    new Trip("Trip 2", "Rome", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17)),
                    new Trip("Trip 3", "Tokyo", LocalDate.of(2027, 9, 3), LocalDate.of(2027, 9, 15))));
        }
    }
}
