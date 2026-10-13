package com.smartmove.backend.service;

import com.smartmove.backend.data.Passenger;
import com.smartmove.backend.data.PassengerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PassengerService {
    private final PassengerRepository repository;

    public PassengerService(PassengerRepository repository) {
        this.repository = repository;
    }

    public List<Passenger> getAllPassengers() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "passengerId"));
    }

    public Passenger createPassenger(Passenger passenger) {
        return repository.save(passenger);
    }
}
