
package com.smartmove.backend.service;

import com.smartmove.backend.data.Vehicle;
import com.smartmove.backend.data.VehicleRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll(
                Sort.by(Sort.Direction.ASC, "vehicleId")
        );
    }

    public Vehicle getVehicleById(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Vehicle was not found"));
    }

    public List<Vehicle> getVehiclesByStatus(String status) {
        return vehicleRepository.findByStatus(status);
    }

    public List<Vehicle> getVehiclesByType(String vehicleType) {
        return vehicleRepository.findByVehicleType(vehicleType);
    }

    public List<Vehicle> getVehiclesByFuelType(String fuelType) {
        return vehicleRepository.findByFuelType(fuelType);
    }

    public Vehicle createVehicle(Vehicle vehicle) {
        if (vehicle.getStatus() == null ||
                vehicle.getStatus().isBlank()) {
            vehicle.setStatus("ACTIVE");
        }

        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicleStatus(Long vehicleId, String status) {
        String normalizedStatus =
                status == null ? "" :
                        status.trim().toUpperCase(Locale.ROOT);

        List<String> validStatuses = List.of(
                "ACTIVE", "INACTIVE", "MAINTENANCE", "RETIRED"
        );

        if (!validStatuses.contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "Status must be ACTIVE, INACTIVE, MAINTENANCE, or RETIRED"
            );
        }

        Vehicle vehicle = getVehicleById(vehicleId);
        vehicle.setStatus(normalizedStatus);

        return vehicleRepository.save(vehicle);
    }
}
