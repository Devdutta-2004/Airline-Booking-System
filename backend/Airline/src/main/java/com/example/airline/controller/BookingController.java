package com.example.airline.controller;

import com.example.airline.entity.Seat;
import com.example.airline.service.BookingService;
import com.example.airline.service.TicketService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;
    private final TicketService ticketService;

    public BookingController(BookingService bookingService, TicketService ticketService) {
        this.bookingService = bookingService;
        this.ticketService = ticketService;
    }

    // ---------------------------------------------------
    // 1) GET SEAT MAP FOR A FLIGHT
    // ---------------------------------------------------
    @GetMapping("/flights/{flightId}/seats")
    public ResponseEntity<List<Seat>> getSeats(@PathVariable Integer flightId) {
        List<Seat> seats = bookingService.getSeatsForFlight(flightId);
        return ResponseEntity.ok(seats);
    }

    // ---------------------------------------------------
    // 2) HOLD SEATS (CREATES BOOKING WITH PENDING STATUS)
    // ---------------------------------------------------
    public static class HoldRequest {
        public Integer userId;
        public Integer flightId;
        public List<String> seats;
        public BigDecimal amount;
    }

    @PostMapping("/book/hold")
    public ResponseEntity<?> holdSeats(@RequestBody HoldRequest req) {

        if (req == null || req.userId == null || req.flightId == null || req.seats == null || req.seats.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid request body"));
        }

        try {
            BookingService.HoldResponse resp =
                    bookingService.holdSeats(req.userId, req.flightId, req.seats, req.amount);

            return ResponseEntity.ok(Map.of(
                    "bookingId", resp.getBookingId(),
                    "pnr", resp.getPnr(),
                    "amount", resp.getAmount(),
                    "expiresAt", resp.getExpiresAt()
            ));

        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
        }
        catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", ex.getMessage()));
        }
    }

    // ---------------------------------------------------
    // 3) PAYMENT CONFIRMATION
    // ---------------------------------------------------
    public static class PaymentConfirmRequest {
        public Integer bookingId;
        public boolean success;
    }

    @PostMapping("/payment/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmRequest req) {
        if (req == null || req.bookingId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid request"));
        }

        try {
            String txnRef = "MOCK-" + UUID.randomUUID().toString().substring(0, 8);
            bookingService.confirmPayment(req.bookingId, req.success, txnRef);
            return ResponseEntity.ok(Map.of("success", req.success));

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", ex.getMessage()));
        }
    }

    // ---------------------------------------------------
    // 4) DOWNLOAD TICKET (PDF)
    // ---------------------------------------------------
    @GetMapping("/booking/{id}/ticket")
    public ResponseEntity<byte[]> downloadTicket(@PathVariable Integer id) {
        try {
            byte[] pdf = ticketService.generateTicketPdfForBooking(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename("ticket_" + id + ".pdf")
                            .build()
            );

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
