package com.smartmove.backend.api;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * Manager-only MongoDB content APIs. Oracle IDs are checked before a MongoDB
 * document is linked to an operational record. Passenger reviews are read-only
 * from the manager content screen; passengers create their own reviews.
 */
@RestController
@RequestMapping("/api/content")
@CrossOrigin(
    origins = {"http://localhost:5173", "http://127.0.0.1:5173"},
    allowCredentials = "true"
)
public class MongoContentController {
    private static final String REVIEWS = "passenger_reviews";
    private static final String ANNOUNCEMENTS = "announcements";
    private static final String VEHICLE_DOCUMENTS = "vehicle_documents";
    private static final String TRIP_MEDIA = "trip_media";

    private final MongoTemplate mongo;
    private final JdbcTemplate jdbc;

    public MongoContentController(MongoTemplate mongo, JdbcTemplate jdbc) {
        this.mongo = mongo;
        this.jdbc = jdbc;
    }

    // -------------------- READ: reviews and rating summaries --------------------

    @GetMapping("/reviews")
    public List<Map<String, Object>> reviews(
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) String keyword) {
        Query query = new Query();
        if (routeId != null) query.addCriteria(Criteria.where("routeId").is(routeId));
        if (keyword != null && !keyword.isBlank()) {
            query.addCriteria(Criteria.where("comments").regex(Pattern.quote(keyword.trim()), "i"));
        }
        query.with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(500);
        return apiRows(mongo.find(query, Document.class, REVIEWS));
    }

    @GetMapping("/reviews/highest-rated-vehicles")
    public List<Map<String, Object>> highestRatedVehicles() {
        List<Document> pipeline = List.of(
            new Document("$match", new Document("vehicleId", new Document("$exists", true))
                .append("rating", new Document("$gte", 1).append("$lte", 5))),
            new Document("$group", new Document("_id", "$vehicleId")
                .append("averageRating", new Document("$avg", "$rating"))
                .append("reviewCount", new Document("$sum", 1))),
            new Document("$sort", new Document("averageRating", -1).append("reviewCount", -1))
        );
        return apiRows(mongo.getCollection(REVIEWS).aggregate(pipeline).into(new ArrayList<>()));
    }

    @GetMapping("/reviews/highest-rated-drivers")
    public List<Map<String, Object>> highestRatedDrivers() {
        List<Document> pipeline = List.of(
            new Document("$match", new Document("driverId", new Document("$exists", true))
                .append("rating", new Document("$gte", 1).append("$lte", 5))),
            new Document("$group", new Document("_id", "$driverId")
                .append("averageRating", new Document("$avg", "$rating"))
                .append("reviewCount", new Document("$sum", 1))),
            new Document("$sort", new Document("averageRating", -1).append("reviewCount", -1))
        );
        return apiRows(mongo.getCollection(REVIEWS).aggregate(pipeline).into(new ArrayList<>()));
    }

    // -------------------- READ: announcements --------------------

    @GetMapping("/announcements")
    public List<Map<String, Object>> announcements(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        Query query = new Query();
        if (activeOnly) {
            Date now = new Date();
            query.addCriteria(new Criteria().andOperator(
                Criteria.where("active").is(true),
                Criteria.where("publishFrom").lte(now),
                Criteria.where("publishUntil").gte(now)
            ));
        }
        query.with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(500);
        return apiRows(mongo.find(query, Document.class, ANNOUNCEMENTS));
    }

    // -------------------- READ: linked vehicle/trip documents --------------------

    @GetMapping("/vehicle-documents")
    public List<Map<String, Object>> allVehicleDocuments(
            @RequestParam(required = false) Long vehicleId) {
        Query query = new Query();
        if (vehicleId != null) query.addCriteria(Criteria.where("vehicleId").is(vehicleId));
        query.with(Sort.by(Sort.Direction.DESC, "uploadedAt")).limit(500);
        return apiRows(mongo.find(query, Document.class, VEHICLE_DOCUMENTS));
    }

    @GetMapping("/vehicles/{vehicleId}/documents")
    public List<Map<String, Object>> vehicleDocuments(@PathVariable long vehicleId) {
        requireOracleRecord("SELECT COUNT(*) FROM vehicle WHERE vehicle_id=?", vehicleId, "Vehicle");
        Query query = Query.query(Criteria.where("vehicleId").is(vehicleId))
            .with(Sort.by(Sort.Direction.DESC, "uploadedAt")).limit(500);
        return apiRows(mongo.find(query, Document.class, VEHICLE_DOCUMENTS));
    }

    @GetMapping("/trip-media")
    public List<Map<String, Object>> allTripMedia(@RequestParam(required = false) Long tripId) {
        Query query = new Query();
        if (tripId != null) query.addCriteria(Criteria.where("tripId").is(tripId));
        query.with(Sort.by(Sort.Direction.DESC, "uploadedAt")).limit(500);
        return apiRows(mongo.find(query, Document.class, TRIP_MEDIA));
    }

    @GetMapping("/trips/{tripId}/media")
    public List<Map<String, Object>> tripMedia(@PathVariable long tripId) {
        requireOracleRecord("SELECT COUNT(*) FROM trip WHERE trip_id=?", tripId, "Trip");
        Query query = Query.query(Criteria.where("tripId").is(tripId))
            .with(Sort.by(Sort.Direction.DESC, "uploadedAt")).limit(500);
        return apiRows(mongo.find(query, Document.class, TRIP_MEDIA));
    }

    // -------------------- CREATE: announcements/documents/media --------------------

    @PostMapping("/announcements")
    public Map<String, Object> createAnnouncement(@RequestBody Map<String, Object> body) {
        long managerId = number(body, "createdByManagerId");
        requireOracleRecord("SELECT COUNT(*) FROM manager WHERE manager_id=?", managerId, "Manager");

        Document doc = new Document("title", required(body, "title"))
            .append("message", required(body, "message"))
            .append("announcementType", optionalText(body.get("announcementType"), "GENERAL"))
            .append("active", booleanValue(body.get("active"), true))
            .append("publishFrom", dateAtStart(required(body, "publishFrom")))
            .append("publishUntil", dateAtEnd(required(body, "publishUntil")))
            .append("createdByManagerId", managerId)
            .append("createdAt", new Date());

        mongo.getCollection(ANNOUNCEMENTS).insertOne(doc);
        return Map.of("message", "Announcement created", "announcementId", doc.getObjectId("_id").toHexString());
    }

    @PostMapping("/vehicles/{vehicleId}/documents")
    public Map<String, Object> addVehicleDocument(
            @PathVariable long vehicleId,
            @RequestBody Map<String, Object> body) {
        requireOracleRecord("SELECT COUNT(*) FROM vehicle WHERE vehicle_id=?", vehicleId, "Vehicle");
        Map<String, Object> vehicle = jdbc.queryForMap(
            "SELECT registration_no FROM vehicle WHERE vehicle_id=?", vehicleId);

        Document doc = new Document("vehicleId", vehicleId)
            .append("registrationNo", String.valueOf(vehicle.get("REGISTRATION_NO")))
            .append("title", required(body, "title"))
            .append("filePath", required(body, "filePath"))
            .append("documentType", required(body, "documentType"))
            .append("uploadedAt", new Date());
        if (body.get("metadata") instanceof Map<?, ?> metadata) doc.append("metadata", safeMetadata(metadata));

        mongo.getCollection(VEHICLE_DOCUMENTS).insertOne(doc);
        return Map.of("message", "Vehicle document metadata created", "documentId", doc.getObjectId("_id").toHexString());
    }

    @PostMapping("/trips/{tripId}/media")
    public Map<String, Object> addTripMedia(
            @PathVariable long tripId,
            @RequestBody Map<String, Object> body) {
        requireOracleRecord("SELECT COUNT(*) FROM trip WHERE trip_id=?", tripId, "Trip");

        Document doc = new Document("tripId", tripId)
            .append("mediaType", required(body, "mediaType"))
            .append("caption", required(body, "caption"))
            .append("filePath", required(body, "filePath"))
            .append("uploadedAt", new Date());
        if (body.get("metadata") instanceof Map<?, ?> metadata) doc.append("metadata", safeMetadata(metadata));

        mongo.getCollection(TRIP_MEDIA).insertOne(doc);
        return Map.of("message", "Trip media metadata created", "mediaId", doc.getObjectId("_id").toHexString());
    }

    // -------------------- UPDATE: mutable content metadata --------------------

    @PutMapping("/announcements/{documentId}")
    public Map<String, Object> updateAnnouncement(
            @PathVariable String documentId,
            @RequestBody Map<String, Object> body) {
        ObjectId id = objectId(documentId);
        Update update = new Update()
            .set("title", required(body, "title"))
            .set("message", required(body, "message"))
            .set("announcementType", optionalText(body.get("announcementType"), "GENERAL"))
            .set("active", booleanValue(body.get("active"), true))
            .set("publishFrom", dateAtStart(required(body, "publishFrom")))
            .set("publishUntil", dateAtEnd(required(body, "publishUntil")));

        UpdateResult result = mongo.updateFirst(Query.query(Criteria.where("_id").is(id)), update, ANNOUNCEMENTS);
        requireMatched(result.getMatchedCount(), "Announcement");
        return Map.of("message", "Announcement updated", "announcementId", documentId);
    }

    @PutMapping("/vehicles/{vehicleId}/documents/{documentId}")
    public Map<String, Object> updateVehicleDocument(
            @PathVariable long vehicleId,
            @PathVariable String documentId,
            @RequestBody Map<String, Object> body) {
        requireOracleRecord("SELECT COUNT(*) FROM vehicle WHERE vehicle_id=?", vehicleId, "Vehicle");
        ObjectId id = objectId(documentId);
        Update update = new Update()
            .set("title", required(body, "title"))
            .set("filePath", required(body, "filePath"))
            .set("documentType", required(body, "documentType"))
            .set("metadata", body.get("metadata") instanceof Map<?, ?> metadata ? safeMetadata(metadata) : Map.of());

        UpdateResult result = mongo.updateFirst(
            Query.query(Criteria.where("_id").is(id).and("vehicleId").is(vehicleId)),
            update, VEHICLE_DOCUMENTS);
        requireMatched(result.getMatchedCount(), "Vehicle document");
        return Map.of("message", "Vehicle document updated", "documentId", documentId);
    }

    @PutMapping("/trips/{tripId}/media/{documentId}")
    public Map<String, Object> updateTripMedia(
            @PathVariable long tripId,
            @PathVariable String documentId,
            @RequestBody Map<String, Object> body) {
        requireOracleRecord("SELECT COUNT(*) FROM trip WHERE trip_id=?", tripId, "Trip");
        ObjectId id = objectId(documentId);
        Update update = new Update()
            .set("mediaType", required(body, "mediaType"))
            .set("caption", required(body, "caption"))
            .set("filePath", required(body, "filePath"))
            .set("metadata", body.get("metadata") instanceof Map<?, ?> metadata ? safeMetadata(metadata) : Map.of());

        UpdateResult result = mongo.updateFirst(
            Query.query(Criteria.where("_id").is(id).and("tripId").is(tripId)),
            update, TRIP_MEDIA);
        requireMatched(result.getMatchedCount(), "Trip media");
        return Map.of("message", "Trip media updated", "mediaId", documentId);
    }

    // -------------------- DELETE: only manager-maintained content --------------------

    @DeleteMapping("/announcements/{documentId}")
    public Map<String, Object> deleteAnnouncement(@PathVariable String documentId) {
        ObjectId id = objectId(documentId);
        DeleteResult result = mongo.remove(Query.query(Criteria.where("_id").is(id)), ANNOUNCEMENTS);
        requireDeleted(result.getDeletedCount(), "Announcement");
        return Map.of("message", "Announcement deleted", "announcementId", documentId);
    }

    @DeleteMapping("/vehicles/{vehicleId}/documents/{documentId}")
    public Map<String, Object> deleteVehicleDocument(
            @PathVariable long vehicleId,
            @PathVariable String documentId) {
        requireOracleRecord("SELECT COUNT(*) FROM vehicle WHERE vehicle_id=?", vehicleId, "Vehicle");
        ObjectId id = objectId(documentId);
        DeleteResult result = mongo.remove(
            Query.query(Criteria.where("_id").is(id).and("vehicleId").is(vehicleId)), VEHICLE_DOCUMENTS);
        requireDeleted(result.getDeletedCount(), "Vehicle document");
        return Map.of("message", "Vehicle document deleted", "documentId", documentId);
    }

    @DeleteMapping("/trips/{tripId}/media/{documentId}")
    public Map<String, Object> deleteTripMedia(
            @PathVariable long tripId,
            @PathVariable String documentId) {
        requireOracleRecord("SELECT COUNT(*) FROM trip WHERE trip_id=?", tripId, "Trip");
        ObjectId id = objectId(documentId);
        DeleteResult result = mongo.remove(
            Query.query(Criteria.where("_id").is(id).and("tripId").is(tripId)), TRIP_MEDIA);
        requireDeleted(result.getDeletedCount(), "Trip media");
        return Map.of("message", "Trip media deleted", "mediaId", documentId);
    }

    // -------------------- Helpers --------------------

    private void requireOracleRecord(String sql, long id, String label) {
        Integer count = jdbc.queryForObject(sql, Integer.class, id);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " ID " + id + " was not found in Oracle.");
        }
    }

    private Map<String, Object> tripReferences(long tripId) {
        try {
            return jdbc.queryForMap("SELECT route_id, vehicle_id, driver_id FROM trip WHERE trip_id=?", tripId);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip was not found.");
        }
    }

    private static ObjectId objectId(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid MongoDB document ID.");
        }
        return new ObjectId(id);
    }

    private static void requireMatched(long count, String label) {
        if (count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, label + " was not found.");
    }

    private static void requireDeleted(long count, String label) {
        if (count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, label + " was not found.");
    }

    private static List<Map<String, Object>> apiRows(List<Document> documents) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Document document : documents) {
            Map<String, Object> row = new LinkedHashMap<>(document);
            Object id = row.get("_id");
            if (id instanceof ObjectId objectId) row.put("_id", objectId.toHexString());
            rows.add(row);
        }
        return rows;
    }

    private static String required(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " is required.");
        }
        return value.toString().trim();
    }

    private static String optionalText(Object value, String fallback) {
        return value == null || value.toString().isBlank() ? fallback : value.toString().trim();
    }

    private static long number(Map<String, Object> body, String key) {
        try {
            return Long.parseLong(required(body, key));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " must be an integer.");
        }
    }

    private static long numberFromRow(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Trip relationship data is incomplete.");
        return ((Number) value).longValue();
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }

    private static Map<String, Object> safeMetadata(Map<?, ?> metadata) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : metadata.entrySet()) {
            if (entry.getKey() != null) result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }

    private static Date dateAtStart(String value) {
        try {
            return Date.from(LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date must use YYYY-MM-DD format.");
        }
    }

    private static Date dateAtEnd(String value) {
        try {
            return Date.from(LocalDate.parse(value).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date must use YYYY-MM-DD format.");
        }
    }
}
