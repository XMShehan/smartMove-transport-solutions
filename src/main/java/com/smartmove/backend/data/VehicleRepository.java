
package com.smartmove.backend.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByStatus(String status);

    List<Vehicle> findByVehicleType(String vehicleType);

    List<Vehicle> findByFuelType(String fuelType);
}
