package com.example.airline.config;

import com.example.airline.entity.Flight;
import com.example.airline.entity.Seat;
import com.example.airline.entity.SeatClass;
import com.example.airline.entity.SeatStatus;
import com.example.airline.repository.FlightRepository;
import com.example.airline.repository.SeatRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SeedSeatsRunner implements CommandLineRunner {

    private final FlightRepository flightRepo;
    private final SeatRepository seatRepo;

    public SeedSeatsRunner(FlightRepository flightRepo, SeatRepository seatRepo) {
        this.flightRepo = flightRepo;
        this.seatRepo = seatRepo;
    }

    @Override
    public void run(String... args) throws Exception {
        List<Flight> flights = flightRepo.findAll();
        for (Flight f : flights) {
            long cnt = seatRepo.findByFlightIdOrderBySeatRowAscSeatColAsc(f.getId().longValue()).size();
            if (cnt > 0) continue; // skip if seats already exist

            int seatsTotal = f.getSeatsTotal() == null ? 30 : f.getSeatsTotal(); // fallback
            int cols = 6;
            int rows = (int) Math.ceil(seatsTotal / (double) cols);

            List<Seat> toSave = new ArrayList<>(rows * cols);
            for (int r = 1; r <= rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Seat s = new Seat();
                    s.setFlightId(f.getId().longValue());
                    char colChar = (char) ('A' + c);
                    String label = r + String.valueOf(colChar);
                    s.setSeatLabel(label);
                    s.setSeatRow(r);
                    s.setSeatCol(String.valueOf(colChar));
                    if (r <= 2) s.setSeatClass(SeatClass.FIRST);
                    else if (r <= 4) s.setSeatClass(SeatClass.BUSINESS);
                    else s.setSeatClass(SeatClass.ECONOMY);
                    s.setStatus(SeatStatus.AVAILABLE);
                    toSave.add(s);
                }
            }
            seatRepo.saveAll(toSave);

            // update flight summary
            f.setSeatsAvailable((int) toSave.size());
            flightRepo.save(f);
            System.out.println("Created " + toSave.size() + " seats for flight " + f.getId());
        }
    }
}
