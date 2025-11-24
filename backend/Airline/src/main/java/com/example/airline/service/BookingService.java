package com.example.airline.service;

import com.example.airline.entity.Booking;
import com.example.airline.entity.BookingSeat;
import com.example.airline.entity.Flight;
import com.example.airline.entity.Payment;
import com.example.airline.entity.PaymentState;
import com.example.airline.entity.Seat;
import com.example.airline.entity.User;
import com.example.airline.repository.BookingRepository;
import com.example.airline.repository.BookingSeatRepository;
import com.example.airline.repository.FlightRepository;
import com.example.airline.repository.PaymentRepository;
import com.example.airline.repository.SeatRepository;
import com.example.airline.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final SeatRepository seatRepo;
    private final BookingRepository bookingRepo;
    private final BookingSeatRepository bookingSeatRepo;
    private final PaymentRepository paymentRepo;
    private final UserRepository userRepo;
    private final FlightRepository flightRepo;

    private static final int HOLD_MINUTES = 10;

    public BookingService(SeatRepository seatRepo,
                          BookingRepository bookingRepo,
                          BookingSeatRepository bookingSeatRepo,
                          PaymentRepository paymentRepo,
                          UserRepository userRepo,
                          FlightRepository flightRepo) {
        this.seatRepo = seatRepo;
        this.bookingRepo = bookingRepo;
        this.bookingSeatRepo = bookingSeatRepo;
        this.paymentRepo = paymentRepo;
        this.userRepo = userRepo;
        this.flightRepo = flightRepo;
    }

    /**
     * Return seats for a flight (ordered by row/col).
     * flightId param is Integer in your domain; Seat.flightId is Long, so convert.
     */
    public List<Seat> getSeatsForFlight(Integer flightId) {
        return seatRepo.findByFlightIdOrderBySeatRowAscSeatColAsc(flightId.longValue());
    }

    /**
     * Hold seats (atomic). Creates booking + booking_seats + payment (PENDING).
     * Uses Integer ids (matches your entities).
     *
     * @param userId    Integer user id (existing in users table)
     * @param flightId  Integer flight id
     * @param seatLabels list of seat labels like ["1A","1B"]
     * @param amount    total amount
     * @return HoldResponse containing booking id, pnr, amount and expiresAt
     */
    @Transactional
    public HoldResponse holdSeats(Integer userId, Integer flightId, List<String> seatLabels, BigDecimal amount) {
        // validate user and flight exist
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Flight flight = flightRepo.findById(flightId)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found: " + flightId));

        if (seatLabels == null || seatLabels.isEmpty()) {
            throw new IllegalArgumentException("No seats requested");
        }

        // expiry for hold
        LocalDateTime expires = LocalDateTime.now().plusMinutes(HOLD_MINUTES);
        List<String> held = new ArrayList<>();

        // Try to hold each seat atomically via repository query that checks status == AVAILABLE
        // SeatRepository methods expect Long flightId because Seat.flightId is Long
        for (String label : seatLabels) {
            int updated = seatRepo.holdSeat(flightId.longValue(), label, expires);
            if (updated != 1) {
                // release any seats we already held in this attempt
                if (!held.isEmpty()) {
                    try {
                        seatRepo.releaseHeldSeats(flightId.longValue(), held);
                    } catch (Exception ignore) {
                    }
                }
                throw new IllegalStateException("Seat not available: " + label);
            }
            held.add(label);
        }

        // Create booking (PENDING). Your Booking entity has User and Flight references.
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setFlight(flight);
        booking.setPaymentStatus("PENDING");    // String field in your Booking entity
        booking.setPnr(generatePnr(flightId));
        booking.setCreatedAt(LocalDateTime.now());

        // --- IMPORTANT: populate NOT NULL fields so DB insert succeeds ---
        booking.setSeatsBooked(seatLabels.size());
        booking.setTotalPrice(amount == null ? 0.0 : amount.doubleValue());
        booking.setStatus("PENDING"); // mark booking as pending while payment is not complete
        // -----------------------------------------------------------------

        booking = bookingRepo.save(booking);   // booking.getId() is Integer

        // Convert booking id (Integer) to Long for booking_seat.bookingId & payment.bookingId
        Long bookingIdLong = booking.getId() == null ? null : booking.getId().longValue();

        // Insert booking_seats
        for (String label : seatLabels) {
            BookingSeat bs = new BookingSeat();
            bs.setBookingId(bookingIdLong);
            bs.setSeatLabel(label);
            bookingSeatRepo.save(bs);
        }

        // Insert payment record (PENDING)
        Payment payment = new Payment();
        payment.setBookingId(bookingIdLong);
        payment.setAmount(amount == null ? BigDecimal.ZERO : amount);
        payment.setMethod("MOCK");
        payment.setStatus(PaymentState.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepo.save(payment);

        return new HoldResponse(booking.getId(), booking.getPnr(), payment.getAmount(), expires);
    }

    /**
     * Confirm mock payment. bookingId is Integer (matches your Booking repo).
     * On success: make HELD -> BOOKED, mark booking.paymentStatus="PAID", payment SUCCESS.
     * On failure: mark payment FAILED and release held seats.
     */
    @Transactional
    public void confirmPayment(Integer bookingId, boolean success, String txnRef) {
        // load booking
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        Long bookingIdLong = booking.getId() == null ? null : booking.getId().longValue();

        // load payment record for the booking
        Payment payment = paymentRepo.findByBookingId(bookingIdLong)
                .orElseThrow(() -> new IllegalStateException("Payment record missing for booking: " + bookingId));

        if (success) {
            // mark payment success
            payment.setStatus(PaymentState.SUCCESS);
            payment.setTxnRef(txnRef);
            paymentRepo.save(payment);

            // update booking status
            booking.setPaymentStatus("PAID");
            bookingRepo.save(booking);

            // find seat labels for the booking
            List<String> labels = bookingSeatRepo.findSeatLabelsByBookingId(bookingIdLong);
            if (labels == null) labels = Collections.emptyList();

            // convert HELD -> BOOKED on seats table
            int changed = 0;
            if (!labels.isEmpty()) {
                changed = seatRepo.bookHeldSeats(booking.getFlight().getId().longValue(), labels, bookingIdLong);
            }

            if (changed != labels.size()) {
                // partly failed to book seats; throw so transaction rolls back
                throw new IllegalStateException("Failed to mark all seats BOOKED (changed=" + changed + " expected=" + labels.size() + ")");
            }

            // ---- IMPORTANT: decrement flight.seatsAvailable summary ----
            Flight flight = booking.getFlight();
            if (flight != null) {
                Integer avail = flight.getSeatsAvailable();
                if (avail == null) avail = 0;
                int bookedCount = labels == null ? 0 : labels.size();
                flight.setSeatsAvailable(Math.max(0, avail - bookedCount));
                flightRepo.save(flight);
            }

        } else {
            // payment failed: mark payment and booking; release held seats
            payment.setStatus(PaymentState.FAILED);
            payment.setTxnRef(txnRef);
            paymentRepo.save(payment);

            booking.setPaymentStatus("FAILED");
            bookingRepo.save(booking);

            List<String> labels = bookingSeatRepo.findSeatLabelsByBookingId(bookingIdLong);
            if (labels != null && !labels.isEmpty()) {
                seatRepo.releaseHeldSeats(booking.getFlight().getId().longValue(), labels);
            }
        }
    }


    private String generatePnr(Integer flightId) {
        String ts = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        String rnd = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 4).toUpperCase();
        String flightPart = (flightId == null) ? "" : String.valueOf(flightId % 100);
        String tail = ts.length() > 6 ? ts.substring(ts.length() - 6) : ts;
        return flightPart + tail + rnd;
    }

    /* HoldResponse DTO (uses Integer bookingId to match Booking.id) */
    public static class HoldResponse {
        private Integer bookingId;
        private String pnr;
        private BigDecimal amount;
        private LocalDateTime expiresAt;

        public HoldResponse(Integer bookingId, String pnr, BigDecimal amount, LocalDateTime expiresAt) {
            this.bookingId = bookingId;
            this.pnr = pnr;
            this.amount = amount;
            this.expiresAt = expiresAt;
        }

        public Integer getBookingId() { return bookingId; }
        public String getPnr() { return pnr; }
        public BigDecimal getAmount() { return amount; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }
}
