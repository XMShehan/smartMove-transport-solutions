-- SAMPLE DATA FOR DEMONSTRATION ONLY.
-- Run as SMARTMOVE after 01_create_schema.sql and 02_booking_triggers.sql.
-- Run this script once on a clean schema. All emails/phone numbers are fictional.
-- Password values are placeholders for database demonstrations only; they are NOT login passwords.

-- 1. Managers
INSERT INTO manager (name, nic, phone, email, password)
VALUES ('Manager One', 'MGR-DEMO-001', '0700000001', 'manager1@smartmove.test', 'DEMO_HASH_ONLY_1');

INSERT INTO manager (name, nic, phone, email, password)
VALUES ('Manager Two', 'MGR-DEMO-002', '0700000002', 'manager2@smartmove.test', 'DEMO_HASH_ONLY_2');

-- 2. Vehicles
INSERT INTO vehicle (manager_id, vehicle_type, registration_no, manufacture_year, seat_capacity, status, fuel_type)
SELECT manager_id, 'BUS', 'WP-NA-1001', 2020, 40, 'ACTIVE', 'DIESEL'
FROM manager WHERE email = 'manager1@smartmove.test';

INSERT INTO vehicle (manager_id, vehicle_type, registration_no, manufacture_year, seat_capacity, status, fuel_type)
SELECT manager_id, 'MINIBUS', 'WP-NA-2002', 2022, 20, 'ACTIVE', 'DIESEL'
FROM manager WHERE email = 'manager1@smartmove.test';

INSERT INTO vehicle (manager_id, vehicle_type, registration_no, manufacture_year, seat_capacity, status, fuel_type)
SELECT manager_id, 'VAN', 'WP-NA-3003', 2021, 12, 'ACTIVE', 'PETROL'
FROM manager WHERE email = 'manager2@smartmove.test';

-- 3. Drivers
INSERT INTO driver (manager_id, name, password, nic, license_no, phone, email, hire_date, status)
SELECT manager_id, 'Driver One', 'DEMO_HASH_ONLY_1', 'DRV-DEMO-001', 'SL-DRV-001', '0710000001', 'driver1@smartmove.test', DATE '2022-01-10', 'ACTIVE'
FROM manager WHERE email = 'manager1@smartmove.test';

INSERT INTO driver (manager_id, name, password, nic, license_no, phone, email, hire_date, status)
SELECT manager_id, 'Driver Two', 'DEMO_HASH_ONLY_2', 'DRV-DEMO-002', 'SL-DRV-002', '0710000002', 'driver2@smartmove.test', DATE '2023-03-15', 'ACTIVE'
FROM manager WHERE email = 'manager1@smartmove.test';

INSERT INTO driver (manager_id, name, password, nic, license_no, phone, email, hire_date, status)
SELECT manager_id, 'Driver Three', 'DEMO_HASH_ONLY_3', 'DRV-DEMO-003', 'SL-DRV-003', '0710000003', 'driver3@smartmove.test', DATE '2024-06-01', 'ACTIVE'
FROM manager WHERE email = 'manager2@smartmove.test';

-- 4. Routes. duration_time is in minutes.
INSERT INTO route (manager_id, route_name, start_location, end_location, distance_km, duration_time, status)
SELECT manager_id, 'Colombo to Kandy', 'Colombo', 'Kandy', 115, 180, 'ACTIVE'
FROM manager WHERE email = 'manager1@smartmove.test';

INSERT INTO route (manager_id, route_name, start_location, end_location, distance_km, duration_time, status)
SELECT manager_id, 'Colombo to Galle', 'Colombo', 'Galle', 125, 150, 'ACTIVE'
FROM manager WHERE email = 'manager1@smartmove.test';

INSERT INTO route (manager_id, route_name, start_location, end_location, distance_km, duration_time, status)
SELECT manager_id, 'Negombo to Colombo', 'Negombo', 'Colombo', 38, 60, 'ACTIVE'
FROM manager WHERE email = 'manager2@smartmove.test';

-- 5. Passengers
INSERT INTO passenger (manager_id, name, nic, email, password, phone, passenger_type, registered_date)
SELECT manager_id, 'Passenger One', 'PAX-DEMO-001', 'passenger1@smartmove.test', 'DEMO_HASH_ONLY_1', '0720000001', 'REGULAR', DATE '2026-01-05'
FROM manager WHERE email = 'manager1@smartmove.test';

INSERT INTO passenger (manager_id, name, nic, email, password, phone, passenger_type, registered_date)
SELECT manager_id, 'Passenger Two', 'PAX-DEMO-002', 'passenger2@smartmove.test', 'DEMO_HASH_ONLY_2', '0720000002', 'REGULAR', DATE '2026-02-10'
FROM manager WHERE email = 'manager1@smartmove.test';

INSERT INTO passenger (manager_id, name, nic, email, password, phone, passenger_type, registered_date)
SELECT manager_id, 'Passenger Three', 'PAX-DEMO-003', 'passenger3@smartmove.test', 'DEMO_HASH_ONLY_3', '0720000003', 'CORPORATE', DATE '2026-03-12'
FROM manager WHERE email = 'manager1@smartmove.test';

INSERT INTO passenger (manager_id, name, nic, email, password, phone, passenger_type, registered_date)
SELECT manager_id, 'Passenger Four', 'PAX-DEMO-004', 'passenger4@smartmove.test', 'DEMO_HASH_ONLY_4', '0720000004', 'REGULAR', DATE '2026-04-20'
FROM manager WHERE email = 'manager2@smartmove.test';

-- 6. Technicians
INSERT INTO technician (manager_id, name, phone, email, password, hire_date, specialization)
SELECT manager_id, 'Technician One', '0730000001', 'technician1@smartmove.test', 'DEMO_HASH_ONLY_1', DATE '2021-05-01', 'Engine and servicing'
FROM manager WHERE email = 'manager1@smartmove.test';

INSERT INTO technician (manager_id, name, phone, email, password, hire_date, specialization)
SELECT manager_id, 'Technician Two', '0730000002', 'technician2@smartmove.test', 'DEMO_HASH_ONLY_2', DATE '2023-08-15', 'Electrical systems'
FROM manager WHERE email = 'manager2@smartmove.test';

-- 7. Trips. available_seats starts at vehicle capacity; booking triggers reduce it.
INSERT INTO trip (departure_time, arrival_time, trip_date, available_seats, status, route_id, vehicle_id, driver_id)
SELECT '08:00', '11:00', DATE '2026-10-10', v.seat_capacity, 'COMPLETED', r.route_id, v.vehicle_id, d.driver_id
FROM route r JOIN vehicle v ON v.registration_no = 'WP-NA-1001' JOIN driver d ON d.license_no = 'SL-DRV-001'
WHERE r.route_name = 'Colombo to Kandy';

INSERT INTO trip (departure_time, arrival_time, trip_date, available_seats, status, route_id, vehicle_id, driver_id)
SELECT '08:00', '11:00', DATE '2026-10-21', v.seat_capacity, 'SCHEDULED', r.route_id, v.vehicle_id, d.driver_id
FROM route r JOIN vehicle v ON v.registration_no = 'WP-NA-1001' JOIN driver d ON d.license_no = 'SL-DRV-001'
WHERE r.route_name = 'Colombo to Kandy';

INSERT INTO trip (departure_time, arrival_time, trip_date, available_seats, status, route_id, vehicle_id, driver_id)
SELECT '07:30', '10:00', DATE '2026-10-11', v.seat_capacity, 'COMPLETED', r.route_id, v.vehicle_id, d.driver_id
FROM route r JOIN vehicle v ON v.registration_no = 'WP-NA-2002' JOIN driver d ON d.license_no = 'SL-DRV-002'
WHERE r.route_name = 'Colombo to Galle';

INSERT INTO trip (departure_time, arrival_time, trip_date, available_seats, status, route_id, vehicle_id, driver_id)
SELECT '09:00', '10:00', DATE '2026-10-20', v.seat_capacity, 'SCHEDULED', r.route_id, v.vehicle_id, d.driver_id
FROM route r JOIN vehicle v ON v.registration_no = 'WP-NA-3003' JOIN driver d ON d.license_no = 'SL-DRV-003'
WHERE r.route_name = 'Negombo to Colombo';

INSERT INTO trip (departure_time, arrival_time, trip_date, available_seats, status, route_id, vehicle_id, driver_id)
SELECT '13:00', '15:30', DATE '2026-10-22', v.seat_capacity, 'SCHEDULED', r.route_id, v.vehicle_id, d.driver_id
FROM route r JOIN vehicle v ON v.registration_no = 'WP-NA-2002' JOIN driver d ON d.license_no = 'SL-DRV-002'
WHERE r.route_name = 'Colombo to Galle';

-- 8. Bookings. The booking trigger reserves seats for all non-cancelled bookings.
INSERT INTO booking (seat_no, price, status, passenger_id, trip_id)
SELECT '1', 2500, 'COMPLETED', p.passenger_id, t.trip_id
FROM passenger p JOIN trip t ON t.trip_date = DATE '2026-10-10' AND t.departure_time = '08:00'
JOIN route r ON r.route_id = t.route_id
WHERE p.email = 'passenger1@smartmove.test' AND r.route_name = 'Colombo to Kandy';

INSERT INTO booking (seat_no, price, status, passenger_id, trip_id)
SELECT '2', 2500, 'COMPLETED', p.passenger_id, t.trip_id
FROM passenger p JOIN trip t ON t.trip_date = DATE '2026-10-10' AND t.departure_time = '08:00'
JOIN route r ON r.route_id = t.route_id
WHERE p.email = 'passenger2@smartmove.test' AND r.route_name = 'Colombo to Kandy';

INSERT INTO booking (seat_no, price, status, passenger_id, trip_id)
SELECT '3', 2500, 'COMPLETED', p.passenger_id, t.trip_id
FROM passenger p JOIN trip t ON t.trip_date = DATE '2026-10-10' AND t.departure_time = '08:00'
JOIN route r ON r.route_id = t.route_id
WHERE p.email = 'passenger3@smartmove.test' AND r.route_name = 'Colombo to Kandy';

INSERT INTO booking (seat_no, price, status, passenger_id, trip_id)
SELECT '1', 2500, 'CONFIRMED', p.passenger_id, t.trip_id
FROM passenger p JOIN trip t ON t.trip_date = DATE '2026-10-21' AND t.departure_time = '08:00'
JOIN route r ON r.route_id = t.route_id
WHERE p.email = 'passenger4@smartmove.test' AND r.route_name = 'Colombo to Kandy';

INSERT INTO booking (seat_no, price, status, passenger_id, trip_id)
SELECT '1', 1800, 'COMPLETED', p.passenger_id, t.trip_id
FROM passenger p JOIN trip t ON t.trip_date = DATE '2026-10-11' AND t.departure_time = '07:30'
JOIN route r ON r.route_id = t.route_id
WHERE p.email = 'passenger1@smartmove.test' AND r.route_name = 'Colombo to Galle';

INSERT INTO booking (seat_no, price, status, passenger_id, trip_id)
SELECT '1', 750, 'CANCELLED', p.passenger_id, t.trip_id
FROM passenger p JOIN trip t ON t.trip_date = DATE '2026-10-20' AND t.departure_time = '09:00'
JOIN route r ON r.route_id = t.route_id
WHERE p.email = 'passenger2@smartmove.test' AND r.route_name = 'Negombo to Colombo';

INSERT INTO booking (seat_no, price, status, passenger_id, trip_id)
SELECT '1', 1800, 'CONFIRMED', p.passenger_id, t.trip_id
FROM passenger p JOIN trip t ON t.trip_date = DATE '2026-10-22' AND t.departure_time = '13:00'
JOIN route r ON r.route_id = t.route_id
WHERE p.email = 'passenger3@smartmove.test' AND r.route_name = 'Colombo to Galle';

-- 9. Payments: one row per booking, matching the selected 1:1 relationship.
-- This example marks completed/confirmed bookings as paid for report demonstration.
INSERT INTO payment (booking_id, manager_id, amount, payment_method, payment_date, payment_status)
SELECT b.booking_id, m.manager_id, b.price, 'CARD', SYSDATE, 'PAID'
FROM booking b CROSS JOIN manager m
WHERE m.email = 'manager1@smartmove.test'
  AND b.status IN ('CONFIRMED','COMPLETED');

-- 10. Maintenance. One record has a next service date in the past for the due-maintenance report.
INSERT INTO maintenance (vehicle_id, technician_id, manager_id, maintenance_type, description, service_date, cost, next_service_date, status)
SELECT v.vehicle_id, t.technician_id, m.manager_id, 'Routine service', 'Oil and safety inspection', DATE '2026-10-01', 18000, DATE '2026-10-10', 'SCHEDULED'
FROM vehicle v CROSS JOIN technician t CROSS JOIN manager m
WHERE v.registration_no = 'WP-NA-1001'
  AND t.email = 'technician1@smartmove.test'
  AND m.email = 'manager1@smartmove.test';

INSERT INTO maintenance (vehicle_id, technician_id, manager_id, maintenance_type, description, service_date, cost, next_service_date, status)
SELECT v.vehicle_id, t.technician_id, m.manager_id, 'Engine check', 'Check engine and cooling system', DATE '2026-10-03', 12000, DATE '2026-11-03', 'SCHEDULED'
FROM vehicle v CROSS JOIN technician t CROSS JOIN manager m
WHERE v.registration_no = 'WP-NA-2002'
  AND t.email = 'technician2@smartmove.test'
  AND m.email = 'manager1@smartmove.test';

INSERT INTO maintenance (vehicle_id, technician_id, manager_id, maintenance_type, description, service_date, cost, next_service_date, status)
SELECT v.vehicle_id, t.technician_id, m.manager_id, 'Electrical inspection', 'Completed electrical inspection', DATE '2026-09-01', 6500, DATE '2026-10-01', 'COMPLETED'
FROM vehicle v CROSS JOIN technician t CROSS JOIN manager m
WHERE v.registration_no = 'WP-NA-3003'
  AND t.email = 'technician2@smartmove.test'
  AND m.email = 'manager2@smartmove.test';

-- 11. Feedback linked to passengers and trips.
INSERT INTO feedback (passenger_id, trip_id, manager_id, rating, description, feedback_date)
SELECT p.passenger_id, t.trip_id, m.manager_id, 5, 'Clean vehicle and helpful driver.', DATE '2026-10-10'
FROM passenger p JOIN trip t ON t.trip_date = DATE '2026-10-10' AND t.departure_time = '08:00'
JOIN route r ON r.route_id = t.route_id
JOIN manager m ON m.email = 'manager1@smartmove.test'
WHERE p.email = 'passenger1@smartmove.test' AND r.route_name = 'Colombo to Kandy';

INSERT INTO feedback (passenger_id, trip_id, manager_id, rating, description, feedback_date)
SELECT p.passenger_id, t.trip_id, m.manager_id, 4, 'Good trip; departed a little late.', DATE '2026-10-10'
FROM passenger p JOIN trip t ON t.trip_date = DATE '2026-10-10' AND t.departure_time = '08:00'
JOIN route r ON r.route_id = t.route_id
JOIN manager m ON m.email = 'manager1@smartmove.test'
WHERE p.email = 'passenger2@smartmove.test' AND r.route_name = 'Colombo to Kandy';

INSERT INTO feedback (passenger_id, trip_id, manager_id, rating, description, feedback_date)
SELECT p.passenger_id, t.trip_id, m.manager_id, 3, 'The trip was fine, but the seats could be more comfortable.', DATE '2026-10-11'
FROM passenger p JOIN trip t ON t.trip_date = DATE '2026-10-11' AND t.departure_time = '07:30'
JOIN route r ON r.route_id = t.route_id
JOIN manager m ON m.email = 'manager1@smartmove.test'
WHERE p.email = 'passenger1@smartmove.test' AND r.route_name = 'Colombo to Galle';

COMMIT;

-- Basic sample-data count check.
SELECT 'MANAGER' table_name, COUNT(*) row_count FROM manager
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
ORDER BY table_name;
