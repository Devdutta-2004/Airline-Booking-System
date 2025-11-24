package com.example.airline.repository;

import com.example.airline.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Integer> {
    // find flights by origin/destination on a given date range
    List<Flight> findByOriginAndDestinationAndDepartureBetween(
            String origin, String destination, LocalDateTime start, LocalDateTime end);
}
