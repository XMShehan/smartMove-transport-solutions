
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
                PasswordHasher.hash(text(b, "password"))
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
                PasswordHasher.hash(text(b, "password")),
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
                PasswordHasher.hash(text(b, "password")),
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
                PasswordHasher.hash(text(b, "password")),
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

        Integer capacity = jdbc.queryForObject(
                "SELECT seat_capacity FROM vehicle WHERE vehicle_id = ?",
                Integer.class,
                vehicleId
        );

        if (capacity == null) {
            throw new IllegalArgumentException(
                    "Vehicle was not found");
        }

        int seats = b.containsKey("availableSeats")
                ? integer(b, "availableSeats")
                : capacity;

        if (seats < 0 || seats > capacity) {
            throw new IllegalArgumentException(
                    "availableSeats must be between 0 and the vehicle seat capacity");
        }

        Map<String, Object> values = row(
                "departure_time", text(b, "departureTime"),
                "arrival_time", text(b, "arrivalTime"),
                "trip_date", sqlDate(b, "tripDate"),
                "available_seats", seats,
                "status", defaultText(b, "status", "SCHEDULED"),
                "route_id", longNumber(b, "routeId"),
                "vehicle_id", vehicleId,
                "driver_id", longNumber(b, "driverId")
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
                        "t.departure_time AS \"departureTime\" " +
                        "FROM booking b " +
                        "JOIN passenger p ON p.passenger_id = b.passenger_id " +
                        "JOIN trip t ON t.trip_id = b.trip_id " +
                        "JOIN route r ON r.route_id = t.route_id " +
                        "ORDER BY b.booking_time DESC"
        );
    }

    @PostMapping("/bookings")
    public Map<String, Object> createBooking(
            @RequestBody Map<String, Object> b) {

        String status = defaultText(
                b, "status", "PENDING").toUpperCase();

        if (!List.of(
                "PENDING", "CONFIRMED",
                "COMPLETED", "CANCELLED"
        ).contains(status)) {
            throw new IllegalArgumentException(
                    "Invalid booking status");
        }

        Map<String, Object> values = row(
                "seat_no", text(b, "seatNo"),
                "price", decimal(b, "price"),
                "status", status,
                "passenger_id", longNumber(b, "passengerId"),
                "trip_id", longNumber(b, "tripId")
        );

        // Oracle trigger manages seat availability.
        return created(
                "bookingId",
                insert("booking", "booking_id", values)
        );
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

        String status = defaultText(
                b, "paymentStatus", "PENDING").toUpperCase();

        if (!List.of(
                "PENDING", "PAID", "FAILED", "REFUNDED"
        ).contains(status)) {
            throw new IllegalArgumentException(
                    "Invalid payment status");
        }

        Map<String, Object> values = row(
                "booking_id", longNumber(b, "bookingId"),
                "manager_id", longNumber(b, "managerId"),
                "amount", decimal(b, "amount"),
                "payment_method", text(b, "paymentMethod"),
                "payment_status", status
        );

        return created(
                "paymentId",
                insert("payment", "payment_id", values)
        );
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
