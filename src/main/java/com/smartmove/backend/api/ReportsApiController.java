package com.smartmove.backend.api;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the coursework Oracle PL/SQL reports to the manager React workspace. */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(
        origins = {"http://localhost:5173", "http://127.0.0.1:5173"},
        allowCredentials = "true")
public class ReportsApiController {
    private static final int MAX_OUTPUT_LINES = 5000;

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public ReportsApiController(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @GetMapping("/most-booked-routes")
    public Map<String, Object> mostBookedRoutes() {
        return runProcedure("Most-booked routes", "BEGIN PR_MOST_BOOKED_ROUTES; END;", statement -> {});
    }

    @GetMapping("/revenue")
    public Map<String, Object> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be on or after start date.");
        }
        return runProcedure("Revenue report", "BEGIN PR_REVENUE_REPORT(?, ?); END;", statement -> {
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
        });
    }

    @GetMapping("/passenger-travel-history/{passengerId}")
    public Map<String, Object> passengerTravelHistory(@PathVariable long passengerId) {
        if (passengerId < 1) throw new IllegalArgumentException("Passenger ID must be positive.");
        return runProcedure("Passenger travel history", "BEGIN PR_PASSENGER_TRAVEL_HISTORY(?); END;",
                statement -> statement.setLong(1, passengerId));
    }

    @GetMapping("/vehicles-due-maintenance")
    public Map<String, Object> vehiclesDueMaintenance() {
        return runProcedure("Vehicles due for maintenance", "BEGIN PR_VEHICLES_DUE_MAINTENANCE; END;", statement -> {});
    }

    @GetMapping("/trip-occupancy")
    public Map<String, Object> tripOccupancy() {
        return runProcedure("Trip occupancy", "BEGIN PR_TRIP_OCCUPANCY_REPORT; END;", statement -> {});
    }

    @GetMapping("/passenger-total-paid/{passengerId}")
    public Map<String, Object> passengerTotalPaid(@PathVariable long passengerId) {
        if (passengerId < 1) throw new IllegalArgumentException("Passenger ID must be positive.");
        BigDecimal total = jdbc.queryForObject(
                "SELECT FN_PASSENGER_TOTAL_PAID(?) FROM dual", BigDecimal.class, passengerId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passengerId", passengerId);
        result.put("totalPaidLkr", total == null ? BigDecimal.ZERO : total);
        result.put("source", "FN_PASSENGER_TOTAL_PAID");
        return result;
    }

    private Map<String, Object> runProcedure(String title, String plsqlBlock, Binder binder) {
        List<String> lines = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            boolean outputEnabled = false;
            try {
                try (CallableStatement enable = connection.prepareCall("BEGIN DBMS_OUTPUT.ENABLE(NULL); END;")) {
                    enable.execute();
                    outputEnabled = true;
                }

                try (CallableStatement procedure = connection.prepareCall(plsqlBlock)) {
                    binder.bind(procedure);
                    procedure.execute();
                }

                try (CallableStatement getLine = connection.prepareCall("BEGIN DBMS_OUTPUT.GET_LINE(?, ?); END;")) {
                    getLine.registerOutParameter(1, Types.VARCHAR);
                    getLine.registerOutParameter(2, Types.INTEGER);
                    while (lines.size() < MAX_OUTPUT_LINES) {
                        getLine.execute();
                        if (getLine.getInt(2) != 0) break;
                        String line = getLine.getString(1);
                        lines.add(line == null ? "" : line);
                    }
                }
            } finally {
                if (outputEnabled) {
                    try (CallableStatement disable = connection.prepareCall("BEGIN DBMS_OUTPUT.DISABLE; END;")) {
                        disable.execute();
                    } catch (SQLException ignored) {
                        // Do not mask a report result/error if cleanup of DBMS_OUTPUT fails.
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Could not execute the Oracle report procedure. Confirm the PL/SQL object is compiled and enabled.", ex);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", title);
        result.put("source", "Oracle PL/SQL");
        result.put("lines", lines);
        if (lines.size() == MAX_OUTPUT_LINES) result.put("truncated", true);
        return result;
    }

    @FunctionalInterface
    private interface Binder {
        void bind(CallableStatement statement) throws SQLException;
    }
}
