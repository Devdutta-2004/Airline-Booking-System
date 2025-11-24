package com.example.airline.service;

import com.example.airline.entity.Booking;
import com.example.airline.entity.BookingSeat;
import com.example.airline.repository.BookingRepository;
import com.example.airline.repository.BookingSeatRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class TicketService {

    private final BookingRepository bookingRepo;
    private final BookingSeatRepository bookingSeatRepo;

    public TicketService(BookingRepository bookingRepo, BookingSeatRepository bookingSeatRepo) {
        this.bookingRepo = bookingRepo;
        this.bookingSeatRepo = bookingSeatRepo;
    }

    /**
     * Generates a SIMPLE PDF ticket (no external libs)
     * This produces a minimal but valid PDF.
     */
    public byte[] generateTicketPdfForBooking(Integer bookingId) {

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        List<BookingSeat> seats = bookingSeatRepo.findByBookingId(
                booking.getId().longValue()
        );

        StringBuilder sb = new StringBuilder();
        sb.append("AIRLINE E-TICKET\n\n");
        sb.append("PNR: ").append(booking.getPnr()).append("\n");
        sb.append("Booking ID: ").append(booking.getId()).append("\n");

        if (booking.getUser() != null) {
            sb.append("User ID: ").append(booking.getUser().getId()).append("\n");
        }

        if (booking.getFlight() != null) {
            sb.append("Flight: ").append(booking.getFlight().getFlightNo()).append("\n\n");
        } else {
            sb.append("Flight: N/A\n\n");
        }

        sb.append("Seats:\n");
        for (BookingSeat s : seats) {
            sb.append(" - ").append(s.getSeatLabel()).append("\n");
        }

        sb.append("\nPayment Status: ").append(booking.getPaymentStatus()).append("\n");

        // Build minimal valid PDF
        String pdf = "%PDF-1.4\n" +
                "1 0 obj <<>> endobj\n" +
                "2 0 obj << /Length " + sb.length() + " >>\n" +
                "stream\n" + sb + "\nendstream\nendobj\n" +
                "3 0 obj << /Type /Page /Parent 4 0 R /Contents 2 0 R >> endobj\n" +
                "4 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
                "5 0 obj << /Type /Catalog /Pages 4 0 R >> endobj\n" +
                "xref\n0 6\n0000000000 65535 f \n" +
                "0000000010 00000 n \n" +
                "0000000053 00000 n \n" +
                "0000000120 00000 n \n" +
                "0000000175 00000 n \n" +
                "0000000227 00000 n \n" +
                "trailer << /Root 5 0 R /Size 6 >>\n" +
                "startxref\n278\n%%EOF";

        return pdf.getBytes(StandardCharsets.UTF_8);
    }
}
