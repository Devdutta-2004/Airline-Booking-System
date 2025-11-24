package com.example.airline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import com.example.airline.repository.FlightRepository;

@SpringBootApplication
public class AirlineApplication {
    public static void main(String[] args) {
        SpringApplication.run(AirlineApplication.class, args);
    }

    // TEMPORARY TEST to verify repository works
    @Bean
    CommandLineRunner smokeTest(FlightRepository flightRepo) {
        return args -> {
            System.out.println("Flight repo bean loaded. Count = " + flightRepo.count());
        };
    }
}
