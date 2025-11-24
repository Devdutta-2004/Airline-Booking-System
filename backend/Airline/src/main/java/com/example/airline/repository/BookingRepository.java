package com.example.airline.repository;

import com.example.airline.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    // Add custom booking queries here later (by user, by flight, etc.)
}
