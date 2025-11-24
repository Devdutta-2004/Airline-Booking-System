package com.example.airline.repository;

import com.example.airline.entity.Seat;
import com.example.airline.entity.SeatStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    // standard finder used by BookingService.getSeatsForFlight()
    List<Seat> findByFlightIdOrderBySeatRowAscSeatColAsc(Long flightId);

    // hold one seat if it's AVAILABLE
    @Modifying
    @Transactional
    @Query("UPDATE Seat s SET s.status = com.example.airline.entity.SeatStatus.HELD, s.holdExpiresAt = :expires WHERE s.flightId = :flightId AND s.seatLabel = :label AND s.status = com.example.airline.entity.SeatStatus.AVAILABLE")
    int holdSeat(@Param("flightId") Long flightId, @Param("label") String label, @Param("expires") LocalDateTime expires);

    // release HELD seats back to AVAILABLE
    @Modifying
    @Transactional
    @Query("UPDATE Seat s SET s.status = com.example.airline.entity.SeatStatus.AVAILABLE, s.holdExpiresAt = NULL WHERE s.flightId = :flightId AND s.seatLabel IN :labels AND s.status = com.example.airline.entity.SeatStatus.HELD")
    int releaseHeldSeats(@Param("flightId") Long flightId, @Param("labels") List<String> labels);

    // convert HELD -> BOOKED and attach booking id
    @Modifying
    @Transactional
    @Query("UPDATE Seat s SET s.status = com.example.airline.entity.SeatStatus.BOOKED, s.bookingId = :bookingId, s.holdExpiresAt = NULL WHERE s.flightId = :flightId AND s.seatLabel IN :labels AND s.status = com.example.airline.entity.SeatStatus.HELD")
    int bookHeldSeats(@Param("flightId") Long flightId, @Param("labels") List<String> labels, @Param("bookingId") Long bookingId);

    // release expired holds (used by scheduler)
    @Modifying
    @Transactional
    @Query("UPDATE Seat s SET s.status = com.example.airline.entity.SeatStatus.AVAILABLE, s.holdExpiresAt = NULL WHERE s.status = com.example.airline.entity.SeatStatus.HELD AND s.holdExpiresAt < :now")
    int releaseExpired(@Param("now") LocalDateTime now);
}
