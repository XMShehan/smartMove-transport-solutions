package com.smartmove.backend.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Self-service profile endpoint. The record ID is always obtained from the server-side session. */
@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class ProfileController {
    private final JdbcTemplate jdbc;

    public ProfileController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, Object> getMyProfile(HttpSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profile", readProfile(currentUser(session)));
        return result;
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateMyProfile(
            @RequestBody Map<String, Object> body, HttpSession session) {
        Map<?, ?> user = currentUser(session);
        String role = String.valueOf(user.get("role"));
        Long id = idFor(user, role);
        String name = text(body.get("name"));
        String nic = text(body.get("nic"));
        String phone = text(body.get("phone"));

        if (name == null) {
            return error(HttpStatus.BAD_REQUEST, "Full name is required.");
        }
        if (name.length() > 100 || (nic != null && nic.length() > 20) || (phone != null && phone.length() > 25)) {
            return error(HttpStatus.BAD_REQUEST, "One or more profile fields exceed the allowed length.");
        }
        if ("MANAGER".equals(role) && nic == null) {
            return error(HttpStatus.BAD_REQUEST, "NIC is required for a manager profile.");
        }

        try {
            int updated;
            if ("MANAGER".equals(role)) {
                updated = jdbc.update("UPDATE manager SET name=?, nic=?, phone=? WHERE manager_id=?", name, nic, phone, id);
            } else {
                updated = jdbc.update("UPDATE passenger SET name=?, nic=?, phone=? WHERE passenger_id=?", name, nic, phone, id);
            }
            if (updated == 0) {
                return error(HttpStatus.NOT_FOUND, "Your profile could not be found. Please sign in again.");
            }

            Map<String, Object> profile = readProfile(user);
            syncSession(session, user, profile);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Profile updated successfully.");
            result.put("profile", profile);
            return ResponseEntity.ok(result);
        } catch (DataIntegrityViolationException ex) {
            return error(HttpStatus.CONFLICT, "That NIC is already registered to another account.");
        }
    }

    private Map<String, Object> readProfile(Map<?, ?> user) {
        String role = String.valueOf(user.get("role"));
        Long id = idFor(user, role);
        List<Map<String, Object>> rows;
        if ("MANAGER".equals(role)) {
            rows = jdbc.queryForList(
                "SELECT manager_id AS \"managerId\", name, nic, phone, email FROM manager WHERE manager_id=?", id);
        } else {
            rows = jdbc.queryForList(
                "SELECT passenger_id AS \"passengerId\", name, nic, phone, email, " +
                "passenger_type AS \"passengerType\", registered_date AS \"registeredDate\" " +
                "FROM passenger WHERE passenger_id=?", id);
        }
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Your profile could not be found. Please sign in again.");
        }
        Map<String, Object> profile = new LinkedHashMap<>(rows.get(0));
        profile.put("role", role);
        return profile;
    }

    private static Map<?, ?> currentUser(HttpSession session) {
        Object raw = session == null ? null : session.getAttribute(AuthController.SESSION_USER);
        if (!(raw instanceof Map<?, ?> user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in to view your profile.");
        }
        Object role = user.get("role");
        if (!"MANAGER".equals(role) && !"PASSENGER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account cannot access a profile.");
        }
        return user;
    }

    private static Long idFor(Map<?, ?> user, String role) {
        Object raw = user.get("MANAGER".equals(role) ? "managerId" : "passengerId");
        if (raw instanceof Number number && number.longValue() > 0) return number.longValue();
        try {
            long id = Long.parseLong(String.valueOf(raw));
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
            // The session is invalid or stale; return the same safe authentication error below.
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your session is missing its account ID. Please sign in again.");
    }

    private static void syncSession(HttpSession session, Map<?, ?> oldUser, Map<String, Object> profile) {
        Map<String, Object> updated = new LinkedHashMap<>();
        oldUser.forEach((key, value) -> {
            if (key instanceof String stringKey) updated.put(stringKey, value);
        });
        updated.put("name", profile.get("name"));
        updated.put("email", profile.get("email"));
        updated.put("nic", profile.get("nic"));
        updated.put("phone", profile.get("phone"));
        session.setAttribute(AuthController.SESSION_USER, updated);
    }

    private static String text(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        return value.toString().trim();
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
