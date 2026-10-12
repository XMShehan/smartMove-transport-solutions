package com.smartmove.backend.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** MongoDB content endpoints. Oracle IDs are checked before documents are linked to them. */
@RestController
@RequestMapping("/api/content")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class MongoContentController {
    private final MongoTemplate mongo;
    private final JdbcTemplate jdbc;

    public MongoContentController(MongoTemplate mongo, JdbcTemplate jdbc) {
        this.mongo = mongo;
        this.jdbc = jdbc;
    }

    @GetMapping("/reviews")
    public List<Document> reviews(@RequestParam(required = false) Long routeId,
                                  @RequestParam(required = false) String keyword) {
        Query query = new Query();
        if (routeId != null) query.addCriteria(Criteria.where("routeId").is(routeId));
        if (keyword != null && !keyword.isBlank()) query.addCriteria(Criteria.where("comments").regex(Pattern.quote(keyword.trim()), "i"));
        query.limit(500);
        return mongo.find(query, Document.class, "passenger_reviews");
    }

    @GetMapping("/reviews/highest-rated-vehicles")
    public List<Document> highestRatedVehicles() {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("vehicleId", new Document("$exists", true)).append("rating", new Document("$gte", 1).append("$lte", 5))),
                new Document("$group", new Document("_id", "$vehicleId").append("averageRating", new Document("$avg", "$rating")).append("reviewCount", new Document("$sum", 1))),
                new Document("$sort", new Document("averageRating", -1).append("reviewCount", -1)));
        return mongo.getCollection("passenger_reviews").aggregate(pipeline).into(new ArrayList<>());
    }

    @GetMapping("/reviews/highest-rated-drivers")
    public List<Document> highestRatedDrivers() {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("driverId", new Document("$exists", true)).append("rating", new Document("$gte", 1).append("$lte", 5))),
                new Document("$group", new Document("_id", "$driverId").append("averageRating", new Document("$avg", "$rating")).append("reviewCount", new Document("$sum", 1))),
                new Document("$sort", new Document("averageRating", -1).append("reviewCount", -1)));
        return mongo.getCollection("passenger_reviews").aggregate(pipeline).into(new ArrayList<>());
    }

    @GetMapping("/vehicles/{vehicleId}/documents")
    public List<Document> vehicleDocuments(@PathVariable long vehicleId) {
        return mongo.find(Query.query(Criteria.where("vehicleId").is(vehicleId)), Document.class, "vehicle_documents");
    }

    @GetMapping("/trips/{tripId}/media")
    public List<Document> tripMedia(@PathVariable long tripId) {
        return mongo.find(Query.query(Criteria.where("tripId").is(tripId)), Document.class, "trip_media");
    }

    @GetMapping("/announcements")
    public List<Document> announcements(@RequestParam(defaultValue = "true") boolean activeOnly) {
        Query query = new Query();
        if (activeOnly) {
            Date now = new Date();
            query.addCriteria(new Criteria().andOperator(
                    Criteria.where("active").is(true),
                    Criteria.where("publishFrom").lte(now),
                    Criteria.where("publishUntil").gte(now)));
        }
        query.limit(500);
        return mongo.find(query, Document.class, "announcements");
    }

    @PostMapping("/reviews")
    public Map<String, Object> createReview(@RequestBody Map<String, Object> body) {
        long passengerId = number(body, "passengerId");
        long tripId = number(body, "tripId");
        int rating = (int) number(body, "rating");
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("rating must be between 1 and 5");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM passenger WHERE passenger_id=?", Integer.class, passengerId) == 0) throw new IllegalArgumentException("passengerId was not found in Oracle");
        Map<String, Object> trip = jdbc.queryForMap("SELECT route_id, vehicle_id, driver_id FROM trip WHERE trip_id=?", tripId);
        String category = body.get("category") == null ? "review" : body.get("category").toString();
        String comments = body.get("comments") == null ? "" : body.get("comments").toString().trim();
        if (comments.isBlank()) throw new IllegalArgumentException("comments is required");
        Document doc = new Document("passengerId", passengerId)
                .append("tripId", tripId)
                .append("routeId", ((Number) trip.get("ROUTE_ID")).longValue())
                .append("vehicleId", ((Number) trip.get("VEHICLE_ID")).longValue())
                .append("driverId", ((Number) trip.get("DRIVER_ID")).longValue())
                .append("rating", rating).append("category", category).append("comments", comments)
                .append("createdAt", new Date());
        mongo.getCollection("passenger_reviews").insertOne(doc);
        return Map.of("message", "Review created", "reviewId", doc.getObjectId("_id").toHexString());
    }

    @PostMapping("/vehicles/{vehicleId}/documents")
    public Map<String, Object> addVehicleDocument(@PathVariable long vehicleId, @RequestBody Map<String, Object> body) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM vehicle WHERE vehicle_id=?", Integer.class, vehicleId) == 0) throw new IllegalArgumentException("vehicleId was not found in Oracle");
        String title = required(body, "title");
        String filePath = required(body, "filePath");
        String documentType = required(body, "documentType");
        Document doc = new Document("vehicleId", vehicleId).append("title", title).append("filePath", filePath)
                .append("documentType", documentType).append("uploadedAt", new Date());
        if (body.get("registrationNo") != null) doc.append("registrationNo", body.get("registrationNo").toString());
        if (body.get("metadata") instanceof Map<?, ?> metadata) doc.append("metadata", metadata);
        mongo.getCollection("vehicle_documents").insertOne(doc);
        return Map.of("message", "Vehicle document metadata created", "documentId", doc.getObjectId("_id").toHexString());
    }

    @PostMapping("/trips/{tripId}/media")
    public Map<String, Object> addTripMedia(@PathVariable long tripId, @RequestBody Map<String, Object> body) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM trip WHERE trip_id=?", Integer.class, tripId) == 0) throw new IllegalArgumentException("tripId was not found in Oracle");
        Document doc = new Document("tripId", tripId).append("mediaType", required(body, "mediaType"))
                .append("caption", required(body, "caption")).append("filePath", required(body, "filePath"))
                .append("uploadedAt", new Date());
        if (body.get("metadata") instanceof Map<?, ?> metadata) doc.append("metadata", metadata);
        mongo.getCollection("trip_media").insertOne(doc);
        return Map.of("message", "Trip media metadata created", "mediaId", doc.getObjectId("_id").toHexString());
    }

    @PostMapping("/announcements")
    public Map<String, Object> createAnnouncement(@RequestBody Map<String, Object> body) {
        long managerId = number(body, "createdByManagerId");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM manager WHERE manager_id=?", Integer.class, managerId) == 0) throw new IllegalArgumentException("createdByManagerId was not found in Oracle");
        Date now = new Date();
        Document doc = new Document("title", required(body, "title"))
                .append("message", required(body, "message"))
                .append("announcementType", body.getOrDefault("announcementType", "GENERAL").toString())
                .append("active", body.getOrDefault("active", Boolean.TRUE))
                .append("publishFrom", body.get("publishFrom") == null ? now : java.sql.Date.valueOf(body.get("publishFrom").toString()))
                .append("publishUntil", body.get("publishUntil") == null ? java.sql.Date.valueOf(java.time.LocalDate.now().plusMonths(1)) : java.sql.Date.valueOf(body.get("publishUntil").toString()))
                .append("createdByManagerId", managerId).append("createdAt", now);
        mongo.getCollection("announcements").insertOne(doc);
        return Map.of("message", "Announcement created", "announcementId", doc.getObjectId("_id").toHexString());
    }

    private static String required(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || value.toString().isBlank()) throw new IllegalArgumentException(key + " is required");
        return value.toString().trim();
    }

    private static long number(Map<String, Object> body, String key) {
        try { return Long.parseLong(required(body, key)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(key + " must be an integer"); }
    }
}
