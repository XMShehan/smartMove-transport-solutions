package com.smartmove.backend.service;

import com.smartmove.backend.data.Driver;
import com.smartmove.backend.data.DriverRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class DriverService {
    private final DriverRepository repository;

    public DriverService(DriverRepository repository) {
        this.repository = repository;
    }

    public List<Driver> getAllDrivers() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "driverId"));
    }

    public Driver createDriver(Driver driver) {
        String status = driver.getStatus() == null ? "ACTIVE" : driver.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!List.of("ACTIVE", "INACTIVE", "ON_LEAVE").contains(status)) {
            throw new IllegalArgumentException("Driver status must be ACTIVE, INACTIVE, or ON_LEAVE");
        }
        return repository.save(driver);
    }
}
