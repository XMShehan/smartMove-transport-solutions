
package com.smartmove.backend.controller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatabaseCheckController {

    private final JdbcTemplate jdbcTemplate;
    private final MongoTemplate mongoTemplate;

    public DatabaseCheckController(
            JdbcTemplate jdbcTemplate,
            MongoTemplate mongoTemplate) {

        this.jdbcTemplate = jdbcTemplate;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/api/health/databases")
    public Map<String, Object> checkDatabases() {

        Map<String, Object> response = new LinkedHashMap<>();

        boolean oracleConnected = false;
        boolean mongoConnected = false;

        // Check Oracle
        try {
            Integer result = jdbcTemplate.queryForObject(
                    "SELECT 1 FROM dual",
                    Integer.class
            );

            oracleConnected = Integer.valueOf(1).equals(result);

            response.put(
                    "oracle",
                    oracleConnected ? "connected" : "error"
            );

        } catch (Exception ex) {
            response.put("oracle", "error");
            response.put("oracleError",
                    ex.getClass().getSimpleName());
        }

        // Check MongoDB
        try {
            String databaseName = mongoTemplate.getDb().getName();
            Set<String> collections = mongoTemplate.getCollectionNames();

            mongoConnected = true;

            response.put("mongodb", "connected");
            response.put("mongoDatabase", databaseName);
            response.put("collections", collections);

        } catch (Exception ex) {
            response.put("mongodb", "error");
            response.put("mongoDatabase", "unavailable");
            response.put("collections", Collections.emptyList());
            response.put("mongodbError",
                    ex.getClass().getSimpleName());
        }

        // Overall application database status
        response.put(
                "status",
                oracleConnected && mongoConnected
                        ? "UP"
                        : "DEGRADED"
        );

        return response;
    }
}
