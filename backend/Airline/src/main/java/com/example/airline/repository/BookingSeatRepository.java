package com.example.airline.repository;

import com.example.airline.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    List<BookingSeat> findByBookingId(Long bookingId);

    @Query("SELECT b.seatLabel FROM BookingSeat b WHERE b.bookingId = :bookingId")
    List<String> findSeatLabelsByBookingId(@Param("bookingId") Long bookingId);
}
