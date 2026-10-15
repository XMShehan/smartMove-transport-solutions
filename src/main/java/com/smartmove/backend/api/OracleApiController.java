package com.smartmove.backend.api;

import com.smartmove.backend.data.Driver;
import com.smartmove.backend.data.Manager;
import com.smartmove.backend.data.Passenger;
import com.smartmove.backend.data.Route;
import com.smartmove.backend.data.Technician;
import com.smartmove.backend.data.Vehicle;

import com.smartmove.backend.service.DriverService;
import com.smartmove.backend.service.ManagerService;
import com.smartmove.backend.service.PassengerService;
import com.smartmove.backend.service.RouteService;
import com.smartmove.backend.service.TechnicianService;
import com.smartmove.backend.service.VehicleService;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(
        origins = {
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        },
        allowCredentials = "true"
)
public class OracleApiController {

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    private final ManagerService managerService;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final RouteService routeService;
    private final PassengerService passengerService;
    private final TechnicianService technicianService;

    public OracleApiController(
            JdbcTemplate jdbc,
            DataSource dataSource,
            ManagerService managerService,
            VehicleService vehicleService,
            DriverService driverService,
            RouteService routeService,
            PassengerService passengerService,
            TechnicianService technicianService) {

        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.managerService = managerService;
        this.vehicleService = vehicleService;
        this.driverService = driverService;
        this.routeService = routeService;
        this.passengerService = passengerService;
        this.technicianService = technicianService;
    }

    // =========================================================
    // MANAGERS
    // =========================================================

    @GetMapping("/managers")
    public List<Manager> managers() {
        return managerService.getAllManagers();
    }

    @PostMapping("/managers")
    public Map<String, Object> createManager(
            @RequestBody Map<String, Object> b) {

        Manager manager = new Manager(
                text(b, "name"),
                text(b, "nic"),
                optionalText(b, "phone"),
                text(b, "email"),
                text(b, "password")
        );

        Manager saved = managerService.createManager(manager);

        return created("managerId", saved.getManagerId());
    }

    // =========================================================
    // VEHICLES
    // =========================================================

    @GetMapping("/vehicles")
    public List<Vehicle> vehicles() {
        return vehicleService.getAllVehicles();
    }

    @PostMapping("/vehicles")
    public Map<String, Object> createVehicle(
            @RequestBody Map<String, Object> b) {

        Vehicle vehicle = new Vehicle();

        vehicle.setManagerId(longNumber(b, "managerId"));
        vehicle.setVehicleType(text(b, "vehicleType"));
        vehicle.setRegistrationNo(text(b, "registrationNo"));
        vehicle.setManufactureYear(
                optionalInteger(b, "manufactureYear"));
        vehicle.setSeatCapacity(integer(b, "seatCapacity"));
        vehicle.setStatus(defaultText(b, "status", "ACTIVE"));
        vehicle.setFuelType(optionalText(b, "fuelType"));

        Vehicle saved = vehicleService.createVehicle(vehicle);

        return created("vehicleId", saved.getVehicleId());
    }

    @PatchMapping("/vehicles/{id}/status")
    public Map<String, Object> updateVehicleStatus(
            @PathVariable long id,
            @RequestBody Map<String, Object> b) {

        Vehicle updated = vehicleService.updateVehicleStatus(
                id, text(b, "status"));

        return message(
                "Vehicle status updated",
                "vehicleId",
                updated.getVehicleId()
        );
    }

    // =========================================================
    // DRIVERS
    // =========================================================

    @GetMapping("/drivers")
    public List<Driver> drivers() {
        return driverService.getAllDrivers();
    }

    @PostMapping("/drivers")
    public Map<String, Object> createDriver(
            @RequestBody Map<String, Object> b) {

        Driver driver = new Driver(
                longNumber(b, "managerId"),
                text(b, "name"),
                text(b, "password"),
                text(b, "nic"),
                text(b, "licenseNo"),
                optionalText(b, "phone"),
                optionalText(b, "email"),
                localDate(b, "hireDate"),
                defaultText(b, "status", "ACTIVE")
        );

        Driver saved = driverService.createDriver(driver);

        return created("driverId", saved.getDriverId());
    }

    // =========================================================
    // ROUTES
    // =========================================================

    @GetMapping("/routes")
    public List<Route> routes() {
        return routeService.getAllRoutes();
    }

    @PostMapping("/routes")
    public Map<String, Object> createRoute(
            @RequestBody Map<String, Object> b) {

        Route route = new Route(
                longNumber(b, "managerId"),
                text(b, "routeName"),
                text(b, "startLocation"),
                text(b, "endLocation"),
                decimal(b, "distanceKm"),
                integer(b, "durationTime"),
                defaultText(b, "status", "ACTIVE")
        );

        Route saved = routeService.createRoute(route);

        return created("routeId", saved.getRouteId());
    }

    // =========================================================
    // PASSENGERS
    // =========================================================

    @GetMapping("/passengers")
    public List<Passenger> passengers() {
        return passengerService.getAllPassengers();
    }

    @PostMapping("/passengers")
    public Map<String, Object> createPassenger(
            @RequestBody Map<String, Object> b) {

        Passenger passenger = new Passenger(
                longNumber(b, "managerId"),
                text(b, "name"),
                optionalText(b, "nic"),
                text(b, "email"),
                text(b, "password"),
                optionalText(b, "phone"),
                text(b, "passengerType")
        );

        Passenger saved = passengerService.createPassenger(passenger);

        return created(
                "passengerId",
                saved.getPassengerId()
        );
    }

    // =========================================================
    // TECHNICIANS
    // =========================================================

    @GetMapping("/technicians")
    public List<Technician> technicians() {
        return technicianService.getAllTechnicians();
    }

    @PostMapping("/technicians")
    public Map<String, Object> createTechnician(
            @RequestBody Map<String, Object> b) {

        Technician technician = new Technician(
                longNumber(b, "managerId"),
                text(b, "name"),
                optionalText(b, "phone"),
                optionalText(b, "email"),
                text(b, "password"),
                localDate(b, "hireDate"),
                text(b, "specialization")
        );

        Technician saved =
                technicianService.createTechnician(technician);

        return created(
                "technicianId",
                saved.getTechnicianId()
        );
    }

    // =========================================================
    // TRIPS - Existing JDBC implementation retained
    // =========================================================

    @GetMapping("/trips")
    public List<Map<String, Object>> trips() {
        return jdbc.queryForList(
                "SELECT t.trip_id AS \"tripId\", " +
                        "t.trip_date AS \"tripDate\", " +
                        "t.departure_time AS \"departureTime\", " +
                        "t.arrival_time AS \"arrivalTime\", " +
                        "t.available_seats AS \"availableSeats\", " +
                        "t.status, t.route_id AS \"routeId\", " +
                        "r.route_name AS \"routeName\", " +
                        "t.vehicle_id AS \"vehicleId\", " +
                        "v.registration_no AS \"registrationNo\", " +
                        "t.driver_id AS \"driverId\", " +
                        "d.name AS \"driverName\" " +
                        "FROM trip t " +
                        "JOIN route r ON r.route_id = t.route_id " +
                        "JOIN vehicle v ON v.vehicle_id = t.vehicle_id " +
                        "JOIN driver d ON d.driver_id = t.driver_id " +
                        "ORDER BY t.trip_date, t.departure_time"
        );
    }

    @PostMapping("/trips")
    public Map<String, Object> createTrip(
            @RequestBody Map<String, Object> b) {

        long vehicleId = longNumber(b, "vehicleId");
        long routeId = longNumber(b, "routeId");
        long driverId = longNumber(b, "driverId");

        List<TripAssignmentInfo> assignments = jdbc.query(
                "SELECT v.seat_capacity, v.status, r.status, d.status " +
                        "FROM vehicle v, route r, driver d " +
                        "WHERE v.vehicle_id = ? AND r.route_id = ? AND d.driver_id = ?",
                (rs, rowNum) -> new TripAssignmentInfo(
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                vehicleId, routeId, driverId);

        if (assignments.isEmpty()) {
            throw new IllegalArgumentException(
                    "Choose an existing vehicle, route, and driver from the available lists.");
        }
        TripAssignmentInfo assignment = assignments.get(0);
        if (!"ACTIVE".equalsIgnoreCase(assignment.vehicleStatus())
                || !"ACTIVE".equalsIgnoreCase(assignment.routeStatus())
                || !"ACTIVE".equalsIgnoreCase(assignment.driverStatus())) {
            throw new IllegalArgumentException(
                    "A trip can only be assigned to an active vehicle, route, and driver.");
        }
        int capacity = assignment.seatCapacity();

        int seats = b.containsKey("availableSeats")
                ? integer(b, "availableSeats")
                : capacity;

        if (seats < 0 || seats > capacity) {
            throw new IllegalArgumentException(
                    "Available seats must be between 0 and the vehicle seat capacity (" + capacity + ").");
        }

        Map<String, Object> values = row(
                "departure_time", text(b, "departureTime"),
                "arrival_time", text(b, "arrivalTime"),
                "trip_date", sqlDate(b, "tripDate"),
                "available_seats", seats,
                "status", defaultText(b, "status", "SCHEDULED"),
                "route_id", routeId,
                "vehicle_id", vehicleId,
                "driver_id", driverId
        );

        return created(
                "tripId",
                insert("trip", "trip_id", values)
        );
    }

    // =========================================================
    // BOOKINGS - Existing JDBC implementation retained
    // =========================================================

    @GetMapping("/bookings")
    public List<Map<String, Object>> bookings() {
        return jdbc.queryForList(
                "SELECT b.booking_id AS \"bookingId\", " +
                        "b.seat_no AS \"seatNo\", " +
                        "b.booking_time AS \"bookingTime\", " +
                        "b.price, b.status, " +
                        "b.passenger_id AS \"passengerId\", " +
                        "p.name AS \"passengerName\", " +
                        "b.trip_id AS \"tripId\", " +
                        "r.route_name AS \"routeName\", " +
                        "t.trip_date AS \"tripDate\", " +
                        "t.departure_time AS \"departureTime\", " +
                        "pay.payment_status AS \"paymentStatus\" " +
                        "FROM booking b " +
                        "JOIN passenger p ON p.passenger_id = b.passenger_id " +
                        "JOIN trip t ON t.trip_id = b.trip_id " +
                        "JOIN route r ON r.route_id = t.route_id " +
                        "LEFT JOIN payment pay ON pay.booking_id = b.booking_id " +
                        "ORDER BY b.booking_time DESC"
        );
    }

    @PostMapping("/bookings")
    public Map<String, Object> createBooking(
            @RequestBody Map<String, Object> b) {

        long passengerId = longNumber(b, "passengerId");
        long tripId = longNumber(b, "tripId");
        String seatNo = text(b, "seatNo");
        String status = defaultText(b, "status", "PENDING").toUpperCase();

        if (!List.of("PENDING", "CONFIRMED", "COMPLETED", "CANCELLED").contains(status)) {
            throw new IllegalArgumentException("Invalid booking status");
        }

        List<BookingTripInfo> tripRows = jdbc.query(
                "SELECT t.available_seats, t.status, t.trip_date, r.distance_km, " +
                        "v.seat_capacity, v.status, r.status, d.status " +
                        "FROM trip t JOIN route r ON r.route_id=t.route_id " +
                        "JOIN vehicle v ON v.vehicle_id=t.vehicle_id " +
                        "JOIN driver d ON d.driver_id=t.driver_id " +
                        "WHERE t.trip_id=?",
                (rs, rowNum) -> new BookingTripInfo(
                        rs.getInt(1), rs.getString(2), rs.getDate(3).toLocalDate(),
                        rs.getBigDecimal(4), rs.getInt(5), rs.getString(6),
                        rs.getString(7), rs.getString(8)),
                tripId);

        if (tripRows.isEmpty()) {
            throw new IllegalArgumentException("The selected trip does not exist.");
        }
        BookingTripInfo trip = tripRows.get(0);
        if (!"SCHEDULED".equalsIgnoreCase(trip.tripStatus())
                || trip.tripDate().isBefore(LocalDate.now())
                || !"ACTIVE".equalsIgnoreCase(trip.routeStatus())
                || !"ACTIVE".equalsIgnoreCase(trip.vehicleStatus())
                || !"ACTIVE".equalsIgnoreCase(trip.driverStatus())) {
            throw new IllegalArgumentException("Bookings can only be created for future scheduled trips with an active route, vehicle, and driver.");
        }
        if (trip.availableSeats() <= 0) {
            throw new IllegalArgumentException("No seats are available on this trip.");
        }
        if (!seatNo.matches("[0-9]+")) {
            throw new IllegalArgumentException("Seat number must be a positive number.");
        }
        int seatNumber;
        try {
            seatNumber = Integer.parseInt(seatNo);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Seat number is outside the supported range.");
        }
        if (seatNumber < 1 || seatNumber > trip.seatCapacity()) {
            throw new IllegalArgumentException("Seat number must be between 1 and " + trip.seatCapacity() + ".");
        }

        Integer seatTaken = jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking WHERE trip_id=? AND TRIM(UPPER(seat_no))=? AND UPPER(status)<>'CANCELLED'",
                Integer.class, tripId, seatNo.toUpperCase());
        if (seatTaken != null && seatTaken > 0) {
            throw new IllegalArgumentException("That seat is already booked. Choose another seat.");
        }

        // One server-side pricing rule for both manager-created and passenger-created bookings.
        BigDecimal fare = trip.distanceKm()
                .multiply(BigDecimal.valueOf(20))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        Map<String, Object> values = row(
                "seat_no", seatNo,
                "price", fare,
                "status", status,
                "passenger_id", passengerId,
                "trip_id", tripId);

        // Oracle trigger decrements available seats. Oracle constraints remain the final protection
        // against concurrent requests attempting to reserve the same seat.
        return created("bookingId", insert("booking", "booking_id", values));
    }

    @PatchMapping("/bookings/{id}/status")
    public Map<String, Object> updateBookingStatus(
            @PathVariable long id,
            @RequestBody Map<String, Object> b) {

        String status = text(b, "status").toUpperCase();

        if (!List.of(
                "PENDING", "CONFIRMED",
                "COMPLETED", "CANCELLED"
        ).contains(status)) {
            throw new IllegalArgumentException(
                    "Invalid booking status");
        }

        int count = jdbc.update(
                "UPDATE booking SET status = ? WHERE booking_id = ?",
                status, id);

        ensureUpdated(count, "Booking");

        return message("Booking status updated", "bookingId", id);
    }

    // =========================================================
    // PAYMENTS - Existing JDBC implementation retained
    // =========================================================

    @GetMapping("/payments")
    public List<Map<String, Object>> payments() {
        return jdbc.queryForList(
                "SELECT p.payment_id AS \"paymentId\", " +
                        "p.booking_id AS \"bookingId\", " +
                        "p.manager_id AS \"managerId\", p.amount, " +
                        "p.payment_method AS \"paymentMethod\", " +
                        "p.payment_date AS \"paymentDate\", " +
                        "p.payment_status AS \"paymentStatus\", " +
                        "b.passenger_id AS \"passengerId\" " +
                        "FROM payment p " +
                        "JOIN booking b ON b.booking_id = p.booking_id " +
                        "ORDER BY p.payment_date DESC"
        );
    }

    @PostMapping("/payments")
    public Map<String, Object> createPayment(
            @RequestBody Map<String, Object> b) {

        long bookingId = longNumber(b, "bookingId");
        long managerId = longNumber(b, "managerId");
        String paymentMethod = text(b, "paymentMethod").toUpperCase();
        String status = defaultText(b, "paymentStatus", "PENDING").toUpperCase();

        if (!List.of("PENDING", "PAID", "FAILED", "REFUNDED").contains(status)) {
            throw new IllegalArgumentException("Invalid payment status");
        }
        if (!List.of("CASH", "CARD", "BANK_TRANSFER", "ONLINE").contains(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method");
        }

        List<PaymentBookingInfo> bookingRows = jdbc.query(
                "SELECT price, status FROM booking WHERE booking_id=?",
                (rs, rowNum) -> new PaymentBookingInfo(rs.getBigDecimal(1), rs.getString(2)),
                bookingId);
        if (bookingRows.isEmpty()) {
            throw new IllegalArgumentException("The selected booking does not exist.");
        }
        PaymentBookingInfo booking = bookingRows.get(0);
        if ("CANCELLED".equalsIgnoreCase(booking.status())) {
            throw new IllegalArgumentException("A cancelled booking cannot receive a new payment.");
        }
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment WHERE booking_id=?", Integer.class, bookingId);
        if (existing != null && existing > 0) {
            throw new IllegalArgumentException("A payment already exists for this booking. Update its payment status instead.");
        }

        // Payment amount is derived from the fare stored against the booking, not typed independently.
        Map<String, Object> values = row(
                "booking_id", bookingId,
                "manager_id", managerId,
                "amount", booking.price().setScale(2, java.math.RoundingMode.HALF_UP),
                "payment_method", paymentMethod,
                "payment_status", status);

        return created("paymentId", insert("payment", "payment_id", values));
    }

    // =========================================================
    // MAINTENANCE - Existing JDBC implementation retained
    // =========================================================

    @GetMapping("/maintenance")
    public List<Map<String, Object>> maintenance() {
        return jdbc.queryForList(
                "SELECT m.maintenance_id AS \"maintenanceId\", " +
                        "m.vehicle_id AS \"vehicleId\", " +
                        "v.registration_no AS \"registrationNo\", " +
                        "m.technician_id AS \"technicianId\", " +
                        "m.manager_id AS \"managerId\", " +
                        "m.maintenance_type AS \"maintenanceType\", " +
                        "m.description, m.service_date AS \"serviceDate\", " +
                        "m.cost, m.next_service_date AS \"nextServiceDate\", " +
                        "m.status " +
                        "FROM maintenance m " +
                        "JOIN vehicle v ON v.vehicle_id = m.vehicle_id " +
                        "ORDER BY m.service_date DESC"
        );
    }

    @PostMapping("/maintenance")
    public Map<String, Object> createMaintenance(
            @RequestBody Map<String, Object> b) {

        Map<String, Object> values = row(
                "vehicle_id", longNumber(b, "vehicleId"),
                "technician_id", longNumber(b, "technicianId"),
                "manager_id", longNumber(b, "managerId"),
                "maintenance_type", text(b, "maintenanceType"),
                "description", optionalText(b, "description"),
                "service_date", sqlDate(b, "serviceDate"),
                "cost", b.containsKey("cost")
                        ? decimal(b, "cost") : BigDecimal.ZERO,
                "next_service_date", optionalDate(b, "nextServiceDate"),
                "status", defaultText(b, "status", "SCHEDULED")
        );

        return created(
                "maintenanceId",
                insert("maintenance", "maintenance_id", values)
        );
    }

    // =========================================================
    // FEEDBACK - Existing JDBC implementation retained
    // =========================================================

    @GetMapping("/feedback")
    public List<Map<String, Object>> feedback() {
        return jdbc.queryForList(
                "SELECT f.feedback_id AS \"feedbackId\", " +
                        "f.passenger_id AS \"passengerId\", " +
                        "p.name AS \"passengerName\", " +
                        "f.trip_id AS \"tripId\", " +
                        "r.route_name AS \"routeName\", " +
                        "f.manager_id AS \"managerId\", " +
                        "f.rating, f.description, " +
                        "f.feedback_date AS \"feedbackDate\" " +
                        "FROM feedback f " +
                        "JOIN passenger p ON p.passenger_id = f.passenger_id " +
                        "JOIN trip t ON t.trip_id = f.trip_id " +
                        "JOIN route r ON r.route_id = t.route_id " +
                        "ORDER BY f.feedback_date DESC"
        );
    }

    @PostMapping("/feedback")
    public Map<String, Object> createFeedback(
            @RequestBody Map<String, Object> b) {

        int rating = integer(b, "rating");

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                    "rating must be between 1 and 5");
        }

        Map<String, Object> values = row(
                "passenger_id", longNumber(b, "passengerId"),
                "trip_id", longNumber(b, "tripId"),
                "manager_id", longNumber(b, "managerId"),
                "rating", rating,
                "description", optionalText(b, "description")
        );

        return created(
                "feedbackId",
                insert("feedback", "feedback_id", values)
        );
    }

    // =========================================================
    // UPDATE AND DELETE OPERATIONS
    // Field/table/column names below are explicit allow-lists; request data
    // supplies values only and can never supply SQL identifiers.
    // =========================================================

    @PutMapping("/{resource}/{id}")
    public ResponseEntity<Map<String, Object>> updateResource(
            @PathVariable String resource,
            @PathVariable long id,
            @RequestBody Map<String, Object> body) {

        CrudSpec spec = crudSpec(resource);
        if (spec == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "This resource does not support editing."));
        }

        Map<String, Object> updates = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, String> field : spec.columns().entrySet()) {
                String fieldName = field.getKey();
                if (!body.containsKey(fieldName)) continue;

                Object raw = body.get(fieldName);
                // Passwords are never returned to the browser. A blank password means keep the current password unchanged.
                if ("password".equals(fieldName) && (raw == null || raw.toString().isBlank())) continue;
                if (spec.required().contains(fieldName)
                        && (raw == null || raw.toString().isBlank())) {
                    return ResponseEntity.badRequest().body(
                            Map.of("error", fieldName + " is required."));
                }
                updates.put(field.getValue(), toCrudSqlValue(resource, fieldName, raw));
            }

            if (updates.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "No editable fields were supplied."));
            }

            if ("trips".equals(resource) && updates.containsKey("available_seats")) {
                List<Integer> capacities = jdbc.query(
                        "SELECT v.seat_capacity FROM trip t JOIN vehicle v ON v.vehicle_id=t.vehicle_id WHERE t.trip_id=?",
                        (rs, rowNum) -> rs.getInt(1), id);
                if (capacities.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Trip was not found."));
                }
                int capacity = capacities.get(0);
                Integer reserved = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM booking WHERE trip_id=? AND status <> 'CANCELLED'",
                        Integer.class, id);
                int alreadyReserved = reserved == null ? 0 : reserved;
                int available = ((Number) updates.get("available_seats")).intValue();
                if (available < 0 || available > capacity - alreadyReserved) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Available seats must be between 0 and the number of seats not already reserved."));
                }
            }

            if ("bookings".equals(resource) && updates.containsKey("status")) {
                List<String> currentStatuses = jdbc.query(
                        "SELECT status FROM booking WHERE booking_id=?",
                        (rs, rowNum) -> rs.getString(1), id);
                if (currentStatuses.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Booking was not found."));
                }
                String current = currentStatuses.get(0).toUpperCase();
                String target = updates.get("status").toString().toUpperCase();
                Set<String> allowedTransitions = switch (current) {
                    case "PENDING" -> Set.of("PENDING", "CONFIRMED", "CANCELLED");
                    case "CONFIRMED" -> Set.of("CONFIRMED", "COMPLETED", "CANCELLED");
                    case "COMPLETED" -> Set.of("COMPLETED");
                    case "CANCELLED" -> Set.of("CANCELLED");
                    default -> Set.of();
                };
                if (!allowedTransitions.contains(target)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                            "error", "That booking status transition is not allowed. Cancelled and completed bookings cannot be reopened."));
                }
            }

            String assignments = String.join(", ", updates.keySet().stream()
                    .map(column -> column + " = ?").toList());
            List<Object> args = new ArrayList<>(updates.values());
            args.add(id);
            int changed = jdbc.update(
                    "UPDATE " + spec.table() + " SET " + assignments + " WHERE " + spec.idColumn() + " = ?",
                    args.toArray());

            if (changed == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", spec.label() + " was not found."));
            }
            return ResponseEntity.ok(Map.of(
                    "message", spec.label() + " updated successfully.",
                    spec.idField(), id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "The update conflicts with existing records or a database constraint. Check unique values, related IDs, and allowed status values."));
        }
    }

    @DeleteMapping("/{resource}/{id}")
    public ResponseEntity<Map<String, Object>> deleteResource(
            @PathVariable String resource,
            @PathVariable long id) {

        CrudSpec spec = crudSpec(resource);
        if (spec == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "This resource does not support deletion."));
        }
        if (!spec.deletable()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", spec.label() + " records are retained for transaction history. Change their status instead of deleting them."));
        }

        try {
            int changed = jdbc.update(
                    "DELETE FROM " + spec.table() + " WHERE " + spec.idColumn() + " = ?", id);
            if (changed == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", spec.label() + " was not found."));
            }
            return ResponseEntity.ok(Map.of(
                    "message", spec.label() + " deleted successfully.",
                    spec.idField(), id));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "This record cannot be deleted because other records depend on it. Deactivate or update the related records first."));
        }
    }

    private CrudSpec crudSpec(String resource) {
        return switch (resource.toLowerCase()) {
            case "managers" -> new CrudSpec("manager", "manager_id", "managerId", "Manager",
                    Map.of("name", "name", "nic", "nic", "phone", "phone", "email", "email", "password", "password"),
                    Set.of("name", "nic", "email"), true);
            case "vehicles" -> new CrudSpec("vehicle", "vehicle_id", "vehicleId", "Vehicle",
                    Map.of("managerId", "manager_id", "vehicleType", "vehicle_type", "registrationNo", "registration_no",
                            "manufactureYear", "manufacture_year", "seatCapacity", "seat_capacity", "status", "status", "fuelType", "fuel_type"),
                    Set.of("managerId", "vehicleType", "registrationNo", "seatCapacity", "status"), true);
            case "drivers" -> new CrudSpec("driver", "driver_id", "driverId", "Driver",
                    Map.of("managerId", "manager_id", "name", "name", "password", "password", "nic", "nic",
                            "licenseNo", "license_no", "phone", "phone", "email", "email", "hireDate", "hire_date", "status", "status"),
                    Set.of("managerId", "name", "nic", "licenseNo", "hireDate", "status"), true);
            case "routes" -> new CrudSpec("route", "route_id", "routeId", "Route",
                    Map.of("managerId", "manager_id", "routeName", "route_name", "startLocation", "start_location",
                            "endLocation", "end_location", "distanceKm", "distance_km", "durationTime", "duration_time", "status", "status"),
                    Set.of("managerId", "routeName", "startLocation", "endLocation", "distanceKm", "durationTime", "status"), true);
            case "passengers" -> new CrudSpec("passenger", "passenger_id", "passengerId", "Passenger",
                    Map.of("managerId", "manager_id", "name", "name", "nic", "nic", "email", "email",
                            "password", "password", "phone", "phone", "passengerType", "passenger_type"),
                    Set.of("managerId", "name", "email", "passengerType"), true);
            case "technicians" -> new CrudSpec("technician", "technician_id", "technicianId", "Technician",
                    Map.of("managerId", "manager_id", "name", "name", "phone", "phone", "email", "email",
                            "password", "password", "hireDate", "hire_date", "specialization", "specialization"),
                    Set.of("managerId", "name", "hireDate", "specialization"), true);
            case "trips" -> new CrudSpec("trip", "trip_id", "tripId", "Trip",
                    Map.of("tripDate", "trip_date", "departureTime", "departure_time", "arrivalTime", "arrival_time",
                            "availableSeats", "available_seats", "status", "status"),
                    Set.of("tripDate", "departureTime", "arrivalTime", "availableSeats", "status"), true);
            case "bookings" -> new CrudSpec("booking", "booking_id", "bookingId", "Booking",
                    Map.of("status", "status"),
                    Set.of("status"), false);
            case "payments" -> new CrudSpec("payment", "payment_id", "paymentId", "Payment",
                    Map.of("paymentMethod", "payment_method", "paymentStatus", "payment_status"),
                    Set.of("paymentMethod", "paymentStatus"), false);
            case "maintenance" -> new CrudSpec("maintenance", "maintenance_id", "maintenanceId", "Maintenance record",
                    Map.of("vehicleId", "vehicle_id", "technicianId", "technician_id", "managerId", "manager_id",
                            "maintenanceType", "maintenance_type", "description", "description", "serviceDate", "service_date",
                            "cost", "cost", "nextServiceDate", "next_service_date", "status", "status"),
                    Set.of("vehicleId", "technicianId", "managerId", "maintenanceType", "serviceDate", "status"), true);
            case "feedback" -> new CrudSpec("feedback", "feedback_id", "feedbackId", "Feedback",
                    Map.of("rating", "rating", "description", "description"), Set.of("rating"), true);
            default -> null;
        };
    }

    private Object toCrudSqlValue(String resource, String field, Object raw) {
        if (raw == null) return null;
        String value = raw.toString().trim();
        if (value.isEmpty()) return null;
        if ("password".equals(field)) return value;

        Set<String> longFields = Set.of("managerId", "vehicleId", "driverId", "routeId", "passengerId", "tripId", "technicianId");
        Set<String> integerFields = Set.of("manufactureYear", "seatCapacity", "durationTime", "availableSeats", "rating");
        Set<String> decimalFields = Set.of("distanceKm", "price", "amount", "cost");
        Set<String> dateFields = Set.of("tripDate", "hireDate", "serviceDate", "nextServiceDate");

        try {
            if (longFields.contains(field)) return Long.parseLong(value);
            if (integerFields.contains(field)) return Integer.parseInt(value);
            if (decimalFields.contains(field)) return new BigDecimal(value);
            if (dateFields.contains(field)) return Date.valueOf(LocalDate.parse(value));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(field + " has an invalid value or format.");
        }

        if ("status".equals(field) || "paymentStatus".equals(field)
                || "paymentMethod".equals(field) || "passengerType".equals(field)) {
            value = value.toUpperCase();
            validateCrudOption(resource, field, value);
        }
        return value;
    }

    private void validateCrudOption(String resource, String field, String value) {
        Set<String> allowed = null;
        if ("status".equals(field)) {
            allowed = switch (resource) {
                case "vehicles" -> Set.of("ACTIVE", "INACTIVE", "MAINTENANCE", "RETIRED");
                case "drivers" -> Set.of("ACTIVE", "INACTIVE", "ON_LEAVE");
                case "routes" -> Set.of("ACTIVE", "INACTIVE");
                case "trips" -> Set.of("SCHEDULED", "COMPLETED", "CANCELLED");
                case "bookings" -> Set.of("PENDING", "CONFIRMED", "COMPLETED", "CANCELLED");
                case "maintenance" -> Set.of("SCHEDULED", "IN_PROGRESS", "COMPLETED", "OVERDUE");
                default -> null;
            };
        } else if ("paymentStatus".equals(field)) {
            allowed = Set.of("PENDING", "PAID", "FAILED", "REFUNDED");
        } else if ("paymentMethod".equals(field)) {
            allowed = Set.of("CASH", "CARD", "BANK_TRANSFER", "ONLINE");
        } else if ("passengerType".equals(field)) {
            allowed = Set.of("REGULAR", "STUDENT", "CORPORATE");
        }
        if (allowed != null && !allowed.contains(value)) {
            throw new IllegalArgumentException("Invalid " + field + " value: " + value);
        }
    }

    private record TripAssignmentInfo(int seatCapacity, String vehicleStatus, String routeStatus, String driverStatus) {}

    private record BookingTripInfo(int availableSeats, String tripStatus, LocalDate tripDate,
                                   BigDecimal distanceKm, int seatCapacity,
                                   String vehicleStatus, String routeStatus, String driverStatus) {}

    private record PaymentBookingInfo(BigDecimal price, String status) {}

    private record CrudSpec(String table, String idColumn, String idField, String label,
                            Map<String, String> columns, Set<String> required, boolean deletable) {}

    // =========================================================
    // SHARED HELPERS
    // =========================================================

    private Number insert(
            String table,
            String idColumn,
            Map<String, Object> values) {

        String[] columns = values.keySet().toArray(new String[0]);

        return new SimpleJdbcInsert(dataSource)
                .withTableName(table)
                .usingColumns(columns)
                .usingGeneratedKeyColumns(idColumn)
                .executeAndReturnKey(
                        new MapSqlParameterSource(values));
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (int i = 0; i < pairs.length; i += 2) {
            result.put((String) pairs[i], pairs[i + 1]);
        }

        return result;
    }

    private static Map<String, Object> created(
            String idName, Number id) {

        return row(
                "message", "Created successfully",
                idName, id.longValue());
    }

    private static Map<String, Object> message(
            String message, String idName, long id) {

        return row(message == null ? "message" : "message",
                message, idName, id);
    }

    private void ensureUpdated(int count, String entity) {
        if (count == 0) {
            throw new IllegalArgumentException(
                    entity + " was not found");
        }
    }

    private static String text(
            Map<String, Object> b, String key) {

        Object value = b.get(key);

        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(
                    key + " is required");
        }

        return value.toString().trim();
    }

    private static String optionalText(
            Map<String, Object> b, String key) {

        Object value = b.get(key);

        return value == null || value.toString().isBlank()
                ? null : value.toString().trim();
    }

    private static String defaultText(
            Map<String, Object> b,
            String key,
            String fallback) {

        String value = optionalText(b, key);
        return value == null ? fallback : value;
    }

    private static long longNumber(
            Map<String, Object> b, String key) {

        try {
            return Long.parseLong(text(b, key));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    key + " must be an integer");
        }
    }

    private static int integer(
            Map<String, Object> b, String key) {

        try {
            return Integer.parseInt(text(b, key));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    key + " must be an integer");
        }
    }

    private static Integer optionalInteger(
            Map<String, Object> b, String key) {

        String value = optionalText(b, key);
        if (value == null) return null;

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    key + " must be an integer");
        }
    }

    private static BigDecimal decimal(
            Map<String, Object> b, String key) {

        try {
            return new BigDecimal(text(b, key));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    key + " must be numeric");
        }
    }

    private static LocalDate localDate(
            Map<String, Object> b, String key) {

        try {
            return LocalDate.parse(text(b, key));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    key + " must use YYYY-MM-DD format");
        }
    }

    private static Date sqlDate(
            Map<String, Object> b, String key) {

        return Date.valueOf(localDate(b, key));
    }

    private static Date optionalDate(
            Map<String, Object> b, String key) {

        String value = optionalText(b, key);

        if (value == null) return null;

        try {
            return Date.valueOf(LocalDate.parse(value));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    key + " must use YYYY-MM-DD format");
        }
    }
}
