package com.example.airline.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @Column(unique = true)
    private String pnr;

    private String paymentStatus;     // PENDING, PAID, FAILED

    private LocalDateTime createdAt = LocalDateTime.now();

    private Integer seatsBooked;
    private Double totalPrice;

    private String status = "CONFIRMED";

    // Getters & setters
    public Integer getId() { return id; }

    public void setId(Integer id) { this.id = id; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    public Flight getFlight() { return flight; }

    public void setFlight(Flight flight) { this.flight = flight; }

    public String getPnr() { return pnr; }

    public void setPnr(String pnr) { this.pnr = pnr; }

    public String getPaymentStatus() { return paymentStatus; }

    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getSeatsBooked() { return seatsBooked; }

    public void setSeatsBooked(Integer seatsBooked) { this.seatsBooked = seatsBooked; }

    public Double getTotalPrice() { return totalPrice; }

    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }
}
