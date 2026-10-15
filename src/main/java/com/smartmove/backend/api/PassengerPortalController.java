package com.smartmove.backend.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.servlet.http.HttpSession;
import org.bson.Document;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Passenger-only views use the passenger ID in the server-side session. */
@RestController
@RequestMapping("/api/passenger")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class PassengerPortalController {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final MongoTemplate mongo;

    public PassengerPortalController(JdbcTemplate jdbc, DataSource dataSource, MongoTemplate mongo) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.mongo = mongo;
    }

    @GetMapping("/trips")
    public List<Map<String, Object>> availableTrips() {
        return jdbc.queryForList(
            "SELECT t.trip_id AS \"tripId\", t.trip_date AS \"tripDate\", " +
            "t.departure_time AS \"departureTime\", t.arrival_time AS \"arrivalTime\", " +
            "t.available_seats AS \"availableSeats\", r.route_id AS \"routeId\", " +
            "r.route_name AS \"routeName\", r.start_location AS \"startLocation\", " +
            "r.end_location AS \"endLocation\", r.distance_km AS \"distanceKm\", " +
            "v.vehicle_type AS \"vehicleType\", ROUND(r.distance_km * 20, 2) AS \"estimatedFare\" " +
            "FROM trip t JOIN route r ON r.route_id=t.route_id " +
            "JOIN vehicle v ON v.vehicle_id=t.vehicle_id " +
            "JOIN driver d ON d.driver_id=t.driver_id " +
            "WHERE t.status='SCHEDULED' AND t.trip_date >= TRUNC(SYSDATE) " +
            "AND t.available_seats > 0 AND r.status='ACTIVE' " +
            "AND v.status='ACTIVE' AND d.status='ACTIVE' " +
            "ORDER BY t.trip_date, t.departure_time");
    }

    @GetMapping("/bookings")
    public List<Map<String, Object>> myBookings(HttpSession session) {
        long passengerId = passengerId(session);
        return jdbc.queryForList(
            "SELECT b.booking_id AS \"bookingId\", b.seat_no AS \"seatNo\", " +
            "b.booking_time AS \"bookingTime\", b.price, b.status, b.trip_id AS \"tripId\", " +
            "r.route_name AS \"routeName\", t.trip_date AS \"tripDate\", " +
            "t.departure_time AS \"departureTime\", t.arrival_time AS \"arrivalTime\", " +
            "p.payment_status AS \"paymentStatus\" " +
            "FROM booking b JOIN trip t ON t.trip_id=b.trip_id " +
            "JOIN route r ON r.route_id=t.route_id " +
            "LEFT JOIN payment p ON p.booking_id=b.booking_id " +
            "WHERE b.passenger_id=? ORDER BY b.booking_time DESC", passengerId);
    }

    @PostMapping("/bookings")
    public ResponseEntity<Map<String, Object>> createBooking(@RequestBody Map<String, Object> body, HttpSession session) {
        long passengerId = passengerId(session);
        Long tripId = longValue(body.get("tripId"));
        String seatNo = text(body.get("seatNo"));
        if (tripId == null || seatNo == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Choose a trip and enter a seat number."));
        }

        List<TripFare> matches = jdbc.query(
            "SELECT t.available_seats, t.status, t.trip_date, r.distance_km, v.seat_capacity, " +
            "v.status, r.status, d.status " +
            "FROM trip t JOIN route r ON r.route_id=t.route_id " +
            "JOIN vehicle v ON v.vehicle_id=t.vehicle_id " +
            "JOIN driver d ON d.driver_id=t.driver_id WHERE t.trip_id=?",
            (rs, i) -> new TripFare(rs.getInt(1), rs.getString(2), rs.getDate(3).toLocalDate(),
                rs.getBigDecimal(4), rs.getInt(5), rs.getString(6), rs.getString(7), rs.getString(8)), tripId);
        if (matches.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Trip was not found."));
        }
        TripFare trip = matches.get(0);
        if (!"SCHEDULED".equalsIgnoreCase(trip.status()) || trip.tripDate().isBefore(LocalDate.now())
                || !"ACTIVE".equalsIgnoreCase(trip.vehicleStatus())
                || !"ACTIVE".equalsIgnoreCase(trip.routeStatus())
                || !"ACTIVE".equalsIgnoreCase(trip.driverStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "This trip is no longer available for booking."));
        }
        if (trip.availableSeats() <= 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No seats are available on this trip."));
        }
        if (!seatNo.matches("[0-9]+")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Seat number must be a positive number."));
        }
        int number;
        try { number = Integer.parseInt(seatNo); }
        catch (NumberFormatException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Seat number is outside the supported range."));
        }
        if (number < 1 || number > trip.seatCapacity()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Seat number must be between 1 and " + trip.seatCapacity() + "."));
        }
        Integer taken = jdbc.queryForObject(
            "SELECT COUNT(*) FROM booking WHERE trip_id=? AND TRIM(UPPER(seat_no))=? AND UPPER(status)<>'CANCELLED'",
            Integer.class, tripId, seatNo.toUpperCase());
        if (taken != null && taken > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "That seat is already booked. Choose another seat."));
        }

        BigDecimal fare = trip.distanceKm().multiply(BigDecimal.valueOf(20))
            .setScale(2, java.math.RoundingMode.HALF_UP);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passenger_id", passengerId);
        values.put("trip_id", tripId);
        values.put("seat_no", seatNo);
        values.put("price", fare);
        values.put("status", "PENDING");
        try {
            Number id = new SimpleJdbcInsert(dataSource).withTableName("booking")
                .usingColumns("passenger_id", "trip_id", "seat_no", "price", "status")
                .usingGeneratedKeyColumns("booking_id")
                .executeAndReturnKey(new MapSqlParameterSource(values));
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Booking created. Complete payment with the transport operator.",
                "bookingId", id.longValue(), "fareLkr", fare));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "That seat may have just been booked or the trip no longer has capacity. Refresh trips and choose another seat."));
        }
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(@PathVariable long bookingId, HttpSession session) {
        int updated = jdbc.update(
            "UPDATE booking SET status='CANCELLED' WHERE booking_id=? AND passenger_id=? AND status IN ('PENDING','CONFIRMED')",
            bookingId, passengerId(session));
        if (updated == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Booking was not found or can no longer be cancelled."));
        }
        return ResponseEntity.ok(Map.of("message", "Booking cancelled.", "bookingId", bookingId));
    }

    @GetMapping("/announcements")
    public List<Document> activeAnnouncements() {
        Query query = Query.query(Criteria.where("active").is(true)).with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongo.find(query, Document.class, "announcements");
    }

    @GetMapping("/reviews")
    public List<Document> myReviews(HttpSession session) {
        Query query = Query.query(Criteria.where("passengerId").is(passengerId(session)))
            .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongo.find(query, Document.class, "passenger_reviews");
    }

    @PostMapping("/reviews")
    public ResponseEntity<Map<String, Object>> createReview(@RequestBody Map<String, Object> body, HttpSession session) {
        long passengerId = passengerId(session);
        Long tripId = longValue(body.get("tripId"));
        Integer rating = intValue(body.get("rating"));
        String category = text(body.get("category"));
        String comments = text(body.get("comments"));
        if (tripId == null || rating == null || rating < 1 || rating > 5 || comments == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Trip, rating from 1 to 5, and comment are required."));
        }
        Integer hasBooking = jdbc.queryForObject(
            "SELECT COUNT(*) FROM booking WHERE passenger_id=? AND trip_id=? AND status <> 'CANCELLED'",
            Integer.class, passengerId, tripId);
        if (hasBooking == null || hasBooking == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can review a trip after booking it."));
        }
       Map<String, Object> tripInfo = jdbc.queryForMap(
           "SELECT route_id, vehicle_id, driver_id FROM trip WHERE trip_id=?", tripId);
       Document doc = new Document("passengerId", passengerId)
           .append("tripId", tripId)
           .append("routeId", ((Number) tripInfo.get("ROUTE_ID")).longValue())
           .append("vehicleId", ((Number) tripInfo.get("VEHICLE_ID")).longValue())
           .append("driverId", ((Number) tripInfo.get("DRIVER_ID")).longValue())
           .append("rating", rating).append("category", category == null ? "review" : category.toLowerCase())
           .append("comments", comments).append("createdAt", new java.util.Date());
        Document saved = mongo.insert(doc, "passenger_reviews");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Review saved to MongoDB.");
        result.put("review", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    private static long passengerId(HttpSession session) {
        Object raw = session.getAttribute(AuthController.SESSION_USER);
        if (!(raw instanceof Map<?, ?> user) || !"PASSENGER".equals(user.get("role"))
                || !(user.get("passengerId") instanceof Number id)) {
            throw new IllegalArgumentException("Passenger session is missing; sign in again.");
        }
        return id.longValue();
    }

    private static String text(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        return value.toString().trim();
    }

    private static Long longValue(Object value) {
        try { return value == null ? null : Long.parseLong(value.toString()); }
        catch (NumberFormatException ex) { return null; }
    }

    private static Integer intValue(Object value) {
        try { return value == null ? null : Integer.parseInt(value.toString()); }
        catch (NumberFormatException ex) { return null; }
    }

    private record TripFare(int availableSeats, String status, LocalDate tripDate, BigDecimal distanceKm,
                            int seatCapacity, String vehicleStatus, String routeStatus, String driverStatus) {}
}
