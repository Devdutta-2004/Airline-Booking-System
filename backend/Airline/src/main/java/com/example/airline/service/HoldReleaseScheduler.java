package com.example.airline.service;

import com.example.airline.repository.SeatRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class HoldReleaseScheduler {

    private final SeatRepository seatRepo;

    public HoldReleaseScheduler(SeatRepository seatRepo) {
        this.seatRepo = seatRepo;
    }

    // run every 60 seconds to release expired holds
    @Scheduled(fixedDelay = 60000)
    public void releaseExpiredHolds() {
        seatRepo.releaseExpired(LocalDateTime.now());
    }
}
