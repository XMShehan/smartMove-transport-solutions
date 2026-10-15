-- COMMON SQL QUERIES FOR SMARTMOVE
-- Run as SMARTMOVE after the schema and optional sample data are created.
-- You can run one query at a time with Cmd+Enter in SQL Developer.

-- 1. Check your current database user.
SELECT USER AS connected_user FROM dual;

-- 2. List the 11 project tables created in your schema.
SELECT table_name FROM user_tables ORDER BY table_name;

-- 3. Show all vehicles.
SELECT vehicle_id, registration_no, vehicle_type, seat_capacity, status, fuel_type
FROM vehicle
ORDER BY registration_no;

-- 4. Show all routes.
SELECT route_id, route_name, start_location, end_location, distance_km, duration_time, status
FROM route
ORDER BY route_name;

-- 5. Show trip schedule with route, vehicle, and driver.
SELECT t.trip_id,
       r.route_name,
       t.trip_date,
       t.departure_time,
       t.arrival_time,
       v.registration_no,
       d.name AS driver_name,
       t.available_seats,
       t.status
FROM trip t
JOIN route r ON r.route_id = t.route_id
JOIN vehicle v ON v.vehicle_id = t.vehicle_id
JOIN driver d ON d.driver_id = t.driver_id
ORDER BY t.trip_date, t.departure_time;

-- 6. Show upcoming trips which still have seats available.
SELECT t.trip_id, r.route_name, t.trip_date, t.departure_time,
       t.arrival_time, t.available_seats
FROM trip t
JOIN route r ON r.route_id = t.route_id
WHERE t.trip_date >= TRUNC(SYSDATE)
  AND t.status = 'SCHEDULED'
  AND t.available_seats > 0
ORDER BY t.trip_date, t.departure_time;

-- 7. View one passenger's bookings. Change the email to the passenger you need.
SELECT p.name AS passenger_name, b.booking_id, r.route_name,
       t.trip_date, t.departure_time, b.seat_no, b.price, b.status
FROM booking b
JOIN passenger p ON p.passenger_id = b.passenger_id
JOIN trip t ON t.trip_id = b.trip_id
JOIN route r ON r.route_id = t.route_id
WHERE p.email = 'passenger1@smartmove.test'
ORDER BY b.booking_time DESC;

-- 8. View payment information with the related booking and passenger.
SELECT pay.payment_id, pay.booking_id, p.name AS passenger_name,
       pay.amount, pay.payment_method, pay.payment_date, pay.payment_status
FROM payment pay
JOIN booking b ON b.booking_id = pay.booking_id
JOIN passenger p ON p.passenger_id = b.passenger_id
ORDER BY pay.payment_date DESC;

-- 9. Calculate paid revenue for October 2026.
SELECT NVL(SUM(amount), 0) AS total_revenue
FROM payment
WHERE payment_status = 'PAID'
  AND payment_date >= DATE '2026-10-01'
  AND payment_date <  DATE '2026-11-01';

-- 10. Most frequently booked routes (excluding cancelled bookings).
SELECT r.route_name, COUNT(b.booking_id) AS booking_count
FROM route r
JOIN trip t ON t.route_id = r.route_id
JOIN booking b ON b.trip_id = t.trip_id
WHERE b.status <> 'CANCELLED'
GROUP BY r.route_id, r.route_name
ORDER BY booking_count DESC;

-- 11. List vehicles with their next service date.
SELECT v.registration_no, v.vehicle_type, m.maintenance_type,
       m.service_date, m.next_service_date, m.status
FROM vehicle v
JOIN maintenance m ON m.vehicle_id = v.vehicle_id
ORDER BY m.next_service_date;

-- 12. Vehicles due for maintenance.
SELECT v.registration_no, v.vehicle_type, m.maintenance_type,
       m.next_service_date, m.status
FROM maintenance m
JOIN vehicle v ON v.vehicle_id = m.vehicle_id
WHERE m.next_service_date <= TRUNC(SYSDATE)
  AND m.status <> 'COMPLETED'
ORDER BY m.next_service_date;

-- 13. Average rating by route.
SELECT r.route_name, ROUND(AVG(f.rating), 2) AS average_rating,
       COUNT(f.feedback_id) AS feedback_count
FROM route r
JOIN trip t ON t.route_id = r.route_id
JOIN feedback f ON f.trip_id = t.trip_id
GROUP BY r.route_id, r.route_name
ORDER BY average_rating DESC;

-- 14. Show feedback comments with passenger and route details.
SELECT p.name AS passenger_name, r.route_name,
       f.rating, f.description, f.feedback_date
FROM feedback f
JOIN passenger p ON p.passenger_id = f.passenger_id
JOIN trip t ON t.trip_id = f.trip_id
JOIN route r ON r.route_id = t.route_id
ORDER BY f.feedback_date DESC;

-- 15. Count rows in all project tables.
SELECT 'MANAGER' AS table_name, COUNT(*) AS row_count FROM manager
UNION ALL SELECT 'VEHICLE', COUNT(*) FROM vehicle
UNION ALL SELECT 'DRIVER', COUNT(*) FROM driver
UNION ALL SELECT 'ROUTE', COUNT(*) FROM route
UNION ALL SELECT 'PASSENGER', COUNT(*) FROM passenger
UNION ALL SELECT 'TECHNICIAN', COUNT(*) FROM technician
UNION ALL SELECT 'TRIP', COUNT(*) FROM trip
UNION ALL SELECT 'BOOKING', COUNT(*) FROM booking
UNION ALL SELECT 'PAYMENT', COUNT(*) FROM payment
UNION ALL SELECT 'MAINTENANCE', COUNT(*) FROM maintenance
UNION ALL SELECT 'FEEDBACK', COUNT(*) FROM feedback
ORDER BY 1;

-- CRUD PRACTICE TEMPLATES: remove the leading -- and edit values before running.
-- INSERT new route:
-- INSERT INTO route (manager_id, route_name, start_location, end_location, distance_km, duration_time, status)
-- VALUES (1, 'Example route', 'Start', 'End', 20, 40, 'ACTIVE');

-- UPDATE a vehicle status:
-- UPDATE vehicle SET status = 'MAINTENANCE' WHERE registration_no = 'WP-NA-1001';
-- COMMIT;

-- CANCEL a booking (the trigger returns the seat to the trip):
-- UPDATE booking SET status = 'CANCELLED' WHERE booking_id = 1;
-- COMMIT;

-- DELETE is usually not recommended for linked transport records; use status updates instead.
-- Example only, and only if no child record references the route:
-- DELETE FROM route WHERE route_id = 999;
