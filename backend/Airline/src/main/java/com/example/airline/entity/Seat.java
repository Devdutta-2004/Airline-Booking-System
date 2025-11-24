package com.example.airline.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats", uniqueConstraints = {@UniqueConstraint(columnNames = {"flight_id","seat_label"})})
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_id", nullable = false)
    private Long flightId;

    @Column(name = "seat_label", nullable = false)
    private String seatLabel;

    @Column(name = "seat_row")
    private Integer seatRow;

    @Column(name = "seat_col")
    private String seatCol;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_class")
    private SeatClass seatClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SeatStatus status;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }
    public String getSeatLabel() { return seatLabel; }
    public void setSeatLabel(String seatLabel) { this.seatLabel = seatLabel; }
    public Integer getSeatRow() { return seatRow; }
    public void setSeatRow(Integer seatRow) { this.seatRow = seatRow; }
    public String getSeatCol() { return seatCol; }
    public void setSeatCol(String seatCol) { this.seatCol = seatCol; }
    public SeatClass getSeatClass() { return seatClass; }
    public void setSeatClass(SeatClass seatClass) { this.seatClass = seatClass; }
    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }
}
