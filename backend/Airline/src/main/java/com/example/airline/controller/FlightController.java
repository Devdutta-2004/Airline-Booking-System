package com.example.airline.controller;

import com.example.airline.entity.Flight;
import com.example.airline.repository.FlightRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {
    private final FlightRepository repo;
    public FlightController(FlightRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Flight> all() {
        return repo.findAll();
    }

    @GetMapping("/search")
    public List<Flight> search(@RequestParam String origin,
                               @RequestParam String destination,
                               @RequestParam String date) {
        LocalDate d = LocalDate.parse(date); // yyyy-MM-dd
        LocalDateTime start = d.atStartOfDay();
        LocalDateTime end = d.atTime(23,59,59);
        return repo.findByOriginAndDestinationAndDepartureBetween(origin, destination, start, end);
    }
}
