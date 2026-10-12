package com.smartmove.backend.api;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Report endpoints for the React dashboard. The PL/SQL procedures remain in Oracle for CW demonstration. */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class ReportsApiController {
    private final JdbcTemplate jdbc;

    public ReportsApiController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/most-booked-routes")
    public List<Map<String, Object>> mostBookedRoutes() {
        return jdbc.queryForList("SELECT r.route_id AS \"routeId\", r.route_name AS \"routeName\", COUNT(b.booking_id) AS \"bookingCount\" FROM route r JOIN trip t ON t.route_id=r.route_id JOIN booking b ON b.trip_id=t.trip_id WHERE b.status <> 'CANCELLED' GROUP BY r.route_id, r.route_name ORDER BY COUNT(b.booking_id) DESC, r.route_name");
    }

    @GetMapping("/revenue")
    public Map<String, Object> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("endDate must be on or after startDate");
        BigDecimal total = jdbc.queryForObject("SELECT NVL(SUM(amount),0) FROM payment WHERE payment_status='PAID' AND payment_date >= ? AND payment_date < ?",
                BigDecimal.class, Date.valueOf(startDate), Date.valueOf(endDate.plusDays(1)));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("totalPaidRevenue", total == null ? BigDecimal.ZERO : total);
        return result;
    }

    @GetMapping("/passenger-travel-history/{passengerId}")
    public List<Map<String, Object>> passengerTravelHistory(@PathVariable long passengerId) {
        return jdbc.queryForList("SELECT b.booking_id AS \"bookingId\", p.name AS \"passengerName\", r.route_name AS \"routeName\", t.trip_date AS \"tripDate\", t.departure_time AS \"departureTime\", t.arrival_time AS \"arrivalTime\", b.seat_no AS \"seatNo\", b.price, b.status FROM booking b JOIN passenger p ON p.passenger_id=b.passenger_id JOIN trip t ON t.trip_id=b.trip_id JOIN route r ON r.route_id=t.route_id WHERE p.passenger_id=? ORDER BY t.trip_date DESC, t.departure_time DESC", passengerId);
    }

    @GetMapping("/vehicles-due-maintenance")
    public List<Map<String, Object>> vehiclesDueMaintenance() {
        return jdbc.queryForList("SELECT m.maintenance_id AS \"maintenanceId\", v.vehicle_id AS \"vehicleId\", v.registration_no AS \"registrationNo\", v.vehicle_type AS \"vehicleType\", m.maintenance_type AS \"maintenanceType\", m.next_service_date AS \"nextServiceDate\", m.status FROM maintenance m JOIN vehicle v ON v.vehicle_id=m.vehicle_id WHERE m.next_service_date IS NOT NULL AND m.next_service_date <= TRUNC(SYSDATE) AND m.status <> 'COMPLETED' ORDER BY m.next_service_date");
    }

    @GetMapping("/trip-occupancy")
    public List<Map<String, Object>> tripOccupancy() {
        return jdbc.queryForList("SELECT t.trip_id AS \"tripId\", r.route_name AS \"routeName\", t.trip_date AS \"tripDate\", t.departure_time AS \"departureTime\", v.registration_no AS \"registrationNo\", v.seat_capacity AS \"seatCapacity\", COUNT(b.booking_id) AS \"bookedSeats\", ROUND(COUNT(b.booking_id)*100/v.seat_capacity,1) AS \"occupancyPercent\" FROM trip t JOIN route r ON r.route_id=t.route_id JOIN vehicle v ON v.vehicle_id=t.vehicle_id LEFT JOIN booking b ON b.trip_id=t.trip_id AND b.status IN ('CONFIRMED','COMPLETED') GROUP BY t.trip_id,r.route_name,t.trip_date,t.departure_time,v.registration_no,v.seat_capacity ORDER BY t.trip_date,t.departure_time");
    }
}
