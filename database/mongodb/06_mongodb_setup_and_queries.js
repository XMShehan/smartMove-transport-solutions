// SMARTMOVE MONGODB EXAMPLES — run with mongosh, not Oracle SQL Developer.
// Uses a separate database for dynamic/unstructured content required by the CW.
// The numeric Oracle IDs below are demo references. In your application, the Java backend
// should use real IDs returned by Oracle and validate them before saving MongoDB documents.

const sm = db.getSiblingDB('smartmove_content');

// Create the four required collections if they do not already exist.
['vehicle_documents', 'passenger_reviews', 'announcements', 'trip_media'].forEach((name) => {
  if (!sm.getCollectionNames().includes(name)) {
    sm.createCollection(name);
  }
});

// Useful indexes for common lookups.
sm.vehicle_documents.createIndex({ vehicleId: 1 });
sm.passenger_reviews.createIndex({ routeId: 1, createdAt: -1 });
sm.passenger_reviews.createIndex({ vehicleId: 1 });
sm.passenger_reviews.createIndex({ driverId: 1 });
sm.passenger_reviews.createIndex({ comments: 'text', category: 'text' });
sm.announcements.createIndex({ active: 1, publishFrom: 1, publishUntil: 1 });
sm.trip_media.createIndex({ tripId: 1, uploadedAt: -1 });

// Add fictional demo documents only if the collections are empty.
if (sm.vehicle_documents.countDocuments() === 0) {
  sm.vehicle_documents.insertMany([
    {
      vehicleId: 1,
      registrationNo: 'WP-NA-1001',
      documentType: 'REGISTRATION_CERTIFICATE',
      title: 'Vehicle registration certificate',
      filePath: '/uploads/vehicles/WP-NA-1001/registration.pdf',
      uploadedAt: new Date('2026-10-01T09:00:00Z'),
      metadata: { fileType: 'application/pdf', verified: true }
    },
    {
      vehicleId: 1,
      registrationNo: 'WP-NA-1001',
      documentType: 'VEHICLE_IMAGE',
      title: 'Front view of bus',
      filePath: '/uploads/vehicles/WP-NA-1001/front.jpg',
      uploadedAt: new Date('2026-10-01T09:05:00Z'),
      metadata: { fileType: 'image/jpeg', view: 'front' }
    }
  ]);
}

if (sm.passenger_reviews.countDocuments() === 0) {
  sm.passenger_reviews.insertMany([
    {
      feedbackId: 1, passengerId: 1, tripId: 1, routeId: 1,
      vehicleId: 1, driverId: 1, rating: 5,
      category: 'compliment', comments: 'Clean bus and helpful driver. The trip was comfortable.',
      createdAt: new Date('2026-10-10T12:00:00Z'), tags: ['clean', 'helpful']
    },
    {
      feedbackId: 2, passengerId: 2, tripId: 1, routeId: 1,
      vehicleId: 1, driverId: 1, rating: 4,
      category: 'complaint', comments: 'The bus departed late, causing a delay.',
      createdAt: new Date('2026-10-10T12:30:00Z'), tags: ['late', 'delay']
    },
    {
      feedbackId: 3, passengerId: 1, tripId: 3, routeId: 2,
      vehicleId: 2, driverId: 2, rating: 3,
      category: 'suggestion', comments: 'Seats could be more comfortable.',
      createdAt: new Date('2026-10-11T11:00:00Z'), tags: ['seats']
    }
  ]);
}

if (sm.announcements.countDocuments() === 0) {
  sm.announcements.insertMany([
    {
      title: 'Schedule reminder',
      message: 'Please arrive at the pickup point 15 minutes before departure.',
      announcementType: 'TRAVEL_REMINDER',
      active: true,
      publishFrom: new Date('2026-10-01T00:00:00Z'),
      publishUntil: new Date('2026-12-31T23:59:59Z'),
      createdByManagerId: 1,
      createdAt: new Date()
    },
    {
      title: 'Service update',
      message: 'Check your booking details before travelling.',
      announcementType: 'SERVICE_UPDATE',
      active: true,
      publishFrom: new Date('2026-10-01T00:00:00Z'),
      publishUntil: new Date('2026-12-31T23:59:59Z'),
      createdByManagerId: 1,
      createdAt: new Date()
    }
  ]);
}

if (sm.trip_media.countDocuments() === 0) {
  sm.trip_media.insertMany([
    {
      tripId: 1,
      mediaType: 'image',
      caption: 'Bus at the departure point',
      filePath: '/uploads/trips/1/departure.jpg',
      uploadedAt: new Date('2026-10-10T07:45:00Z'),
      metadata: { fileType: 'image/jpeg', sizeBytes: 180000 }
    },
    {
      tripId: 1,
      mediaType: 'video',
      caption: 'Trip announcement video',
      filePath: '/uploads/trips/1/announcement.mp4',
      uploadedAt: new Date('2026-10-10T07:50:00Z'),
      metadata: { fileType: 'video/mp4', sizeBytes: 1200000, durationSeconds: 25 }
    }
  ]);
}

print('\nCollections and sample document counts:');
printjson({
  vehicle_documents: sm.vehicle_documents.countDocuments(),
  passenger_reviews: sm.passenger_reviews.countDocuments(),
  announcements: sm.announcements.countDocuments(),
  trip_media: sm.trip_media.countDocuments()
});

// CW query 1: retrieve all passenger reviews for a particular route (demo routeId 1).
print('\nCW QUERY 1 — Passenger reviews for routeId 1');
sm.passenger_reviews.find(
  { routeId: 1 },
  { _id: 0, passengerId: 1, tripId: 1, rating: 1, category: 1, comments: 1, createdAt: 1 }
).sort({ createdAt: -1 }).forEach(printjson);

// CW query 2A: highest-rated vehicles, ranked by average rating.
print('\nCW QUERY 2A — Highest-rated vehicles');
sm.passenger_reviews.aggregate([
  { $match: { vehicleId: { $exists: true }, rating: { $gte: 1, $lte: 5 } } },
  { $group: { _id: '$vehicleId', averageRating: { $avg: '$rating' }, reviewCount: { $sum: 1 } } },
  { $sort: { averageRating: -1, reviewCount: -1 } }
]).forEach(printjson);

// CW query 2B: highest-rated drivers, ranked by average rating.
print('\nCW QUERY 2B — Highest-rated drivers');
sm.passenger_reviews.aggregate([
  { $match: { driverId: { $exists: true }, rating: { $gte: 1, $lte: 5 } } },
  { $group: { _id: '$driverId', averageRating: { $avg: '$rating' }, reviewCount: { $sum: 1 } } },
  { $sort: { averageRating: -1, reviewCount: -1 } }
]).forEach(printjson);

// CW query 3: search feedback/complaints using keywords. Requires the text index above.
print('\nCW QUERY 3 — Search comments for keywords: late or delay');
sm.passenger_reviews.find(
  { $text: { $search: 'late delay' } },
  { _id: 0, routeId: 1, tripId: 1, rating: 1, category: 1, comments: 1 }
).forEach(printjson);

// CW query 4A: retrieve documents for a specific vehicle (demo vehicleId 1).
print('\nCW QUERY 4A — Vehicle documents for vehicleId 1');
sm.vehicle_documents.find({ vehicleId: 1 }, { _id: 0 }).forEach(printjson);

// CW query 4B: retrieve multimedia for a specific trip (demo tripId 1).
print('\nCW QUERY 4B — Trip media for tripId 1');
sm.trip_media.find({ tripId: 1 }, { _id: 0 }).sort({ uploadedAt: -1 }).forEach(printjson);

// Extra: retrieve currently active announcements.
print('\nEXTRA — Active announcements');
const now = new Date();
sm.announcements.find({ active: true, publishFrom: { $lte: now }, publishUntil: { $gte: now } }, { _id: 0 })
  .sort({ publishFrom: -1 }).forEach(printjson);
