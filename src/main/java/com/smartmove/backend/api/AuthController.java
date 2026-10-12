package com.smartmove.backend.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Session-based login and first-use account setup for the local SmartMove coursework app. */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class AuthController {
    public static final String SESSION_USER = "smartmoveAuthUser";

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public AuthController(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @GetMapping("/setup-status")
    public Map<String, Object> setupStatus() {
        return Map.of("managerSetupAvailable", managerSetupAvailable());
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(SESSION_USER) instanceof Map<?, ?> user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Please sign in."));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> safeUser = (Map<String, Object>) user;
        return ResponseEntity.ok(Map.of("authenticated", true, "user", safeUser));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String email = clean(body.get("email"));
        String password = string(body.get("password"));
        String role = clean(body.get("role")).toUpperCase();
        if (email == null || password == null || password.isBlank() || !(role.equals("MANAGER") || role.equals("PASSENGER"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Enter your email, password, and account type."));
        }

        Account account = null;
        if (role.equals("MANAGER")) {
            List<Account> accounts = jdbc.query(
                "SELECT manager_id, name, email, password FROM manager WHERE LOWER(email)=LOWER(?)",
                (rs, rowNum) -> new Account(rs.getLong("manager_id"), rs.getString("name"), rs.getString("email"), rs.getString("password")), email);
            if (!accounts.isEmpty()) account = accounts.get(0);
        } else {
            List<Account> accounts = jdbc.query(
                "SELECT passenger_id, name, email, password FROM passenger WHERE LOWER(email)=LOWER(?)",
                (rs, rowNum) -> new Account(rs.getLong("passenger_id"), rs.getString("name"), rs.getString("email"), rs.getString("password")), email);
            if (!accounts.isEmpty()) account = accounts.get(0);
        }

        // Coursework sample rows contain DEMO_HASH_ONLY_* placeholders, which intentionally cannot authenticate.
        if (account == null || !PasswordHasher.verify(password, account.passwordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email or password is incorrect. Sample placeholder passwords cannot be used to sign in."));
        }
        Map<String, Object> user = toUser(account.id(), role, account.name(), account.email());
        setSessionUser(request, user);
        return ResponseEntity.ok(Map.of("message", "Signed in successfully.", "user", user));
    }

    /** First-use setup is open only while no manager account has a PBKDF2 password hash. Local coursework use only. */
    @PostMapping("/setup-manager")
    public ResponseEntity<Map<String, Object>> setupFirstManager(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!managerSetupAvailable()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Initial manager setup has already been completed. Sign in with the manager account."));
        }
        String name = clean(body.get("name"));
        String nic = clean(body.get("nic"));
        String email = clean(body.get("email"));
        String phone = clean(body.get("phone"));
        String password = string(body.get("password"));
        String invalid = validateNameEmailPassword(name, email, password);
        if (invalid != null) return ResponseEntity.badRequest().body(Map.of("error", invalid));
        if (nic == null) return ResponseEntity.badRequest().body(Map.of("error", "NIC or manager identification is required."));
        if (emailInUse("manager", email)) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "That email is already registered as a manager."));

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", name);
        values.put("nic", nic);
        values.put("phone", phone);
        values.put("email", email.toLowerCase());
        values.put("password", PasswordHasher.hash(password));
        Number id = new SimpleJdbcInsert(dataSource).withTableName("manager")
            .usingColumns("name", "nic", "phone", "email", "password")
            .usingGeneratedKeyColumns("manager_id")
            .executeAndReturnKey(new MapSqlParameterSource(values));

        Map<String, Object> user = toUser(id.longValue(), "MANAGER", name, email.toLowerCase());
        setSessionUser(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Manager account created. Keep your login details private.", "user", user));
    }

    @PostMapping("/register-passenger")
    public ResponseEntity<Map<String, Object>> registerPassenger(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String name = clean(body.get("name"));
        String email = clean(body.get("email"));
        String password = string(body.get("password"));
        String nic = clean(body.get("nic"));
        String phone = clean(body.get("phone"));
        String type = clean(body.get("passengerType"));
        String invalid = validateNameEmailPassword(name, email, password);
        if (invalid != null) return ResponseEntity.badRequest().body(Map.of("error", invalid));
        if (emailInUse("passenger", email)) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "That email is already registered as a passenger."));

        List<Long> managers = jdbc.query("SELECT manager_id FROM manager WHERE password LIKE 'pbkdf2-sha256$%' ORDER BY manager_id FETCH FIRST 1 ROWS ONLY",
            (rs, rowNum) -> rs.getLong(1));
        if (managers.isEmpty()) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Initial manager setup must be completed before passenger registration."));

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("manager_id", managers.get(0));
        values.put("name", name);
        values.put("nic", nic);
        values.put("email", email.toLowerCase());
        values.put("password", PasswordHasher.hash(password));
        values.put("phone", phone);
        values.put("passenger_type", (type == null || type.isBlank()) ? "REGULAR" : type.toUpperCase());
        Number id = new SimpleJdbcInsert(dataSource).withTableName("passenger")
            .usingColumns("manager_id", "name", "nic", "email", "password", "phone", "passenger_type")
            .usingGeneratedKeyColumns("passenger_id")
            .executeAndReturnKey(new MapSqlParameterSource(values));

        Map<String, Object> user = toUser(id.longValue(), "PASSENGER", name, email.toLowerCase());
        setSessionUser(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Passenger account created.", "user", user));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return Map.of("message", "Signed out.");
    }

    private boolean managerSetupAvailable() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM manager WHERE password LIKE 'pbkdf2-sha256$%'", Integer.class);
        return count == null || count == 0;
    }

    private boolean emailInUse(String table, String email) {
        // table is selected only from this controller's constant call sites.
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE LOWER(email)=LOWER(?)", Integer.class, email);
        return count != null && count > 0;
    }

    private static String validateNameEmailPassword(String name, String email, String password) {
        if (name == null || name.isBlank()) return "Name is required.";
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) return "Enter a valid email address.";
        if (password == null || password.length() < 8) return "Password must contain at least 8 characters.";
        return null;
    }

    private static String clean(Object value) {
        if (value == null) return null;
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String string(Object value) { return value == null ? null : value.toString(); }

    private static Map<String, Object> toUser(long id, String role, String name, String email) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", id);
        user.put("role", role);
        user.put("name", name);
        user.put("email", email);
        if (role.equals("MANAGER")) user.put("managerId", id);
        else user.put("passengerId", id);
        return user;
    }

    private static void setSessionUser(HttpServletRequest request, Map<String, Object> user) {
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        request.getSession(true).setAttribute(SESSION_USER, user);
    }

    private record Account(long id, String name, String email, String passwordHash) {}
}
