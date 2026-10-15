-- PL/SQL REPORT EXAMPLES FOR THE DM2 CW
-- Run as SMARTMOVE using F5 / Run Script in SQL Developer (not just Cmd+Enter).
-- First run 01_create_schema.sql, 02_booking_triggers.sql, and 03_sample_data.sql.
-- These are teaching examples. Read each query and practise explaining the cursor and exception handling.

SET SERVEROUTPUT ON SIZE UNLIMITED;

-- Function: total amount paid by one passenger.
CREATE OR REPLACE FUNCTION fn_passenger_total_paid (
    p_passenger_id IN NUMBER
) RETURN NUMBER
IS
    v_total NUMBER(12,2);
BEGIN
    SELECT NVL(SUM(pay.amount), 0)
    INTO v_total
    FROM payment pay
    JOIN booking b ON b.booking_id = pay.booking_id
    WHERE b.passenger_id = p_passenger_id
      AND pay.payment_status = 'PAID';

    RETURN v_total;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Could not calculate passenger total: ' || SQLERRM);
        RAISE;
END;
/

-- Report 1: routes with the most bookings.
CREATE OR REPLACE PROCEDURE pr_most_booked_routes
IS
    v_rows NUMBER := 0;
BEGIN
    DBMS_OUTPUT.PUT_LINE('REPORT 1 - MOST FREQUENTLY BOOKED ROUTES');
    FOR rec IN (
        SELECT r.route_name, COUNT(b.booking_id) AS booking_count
        FROM route r
        JOIN trip t ON t.route_id = r.route_id
        JOIN booking b ON b.trip_id = t.trip_id
        WHERE b.status <> 'CANCELLED'
        GROUP BY r.route_id, r.route_name
        ORDER BY COUNT(b.booking_id) DESC, r.route_name
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(rec.route_name || ' : ' || rec.booking_count || ' bookings');
        v_rows := v_rows + 1;
    END LOOP;

    IF v_rows = 0 THEN
        DBMS_OUTPUT.PUT_LINE('No bookings were found.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error in most-booked-routes report: ' || SQLERRM);
        RAISE;
END;
/

-- Report 2: total paid revenue for a date range.
CREATE OR REPLACE PROCEDURE pr_revenue_report (
    p_start_date IN DATE,
    p_end_date   IN DATE
)
IS
    v_revenue NUMBER(12,2);
BEGIN
    IF p_start_date IS NULL OR p_end_date IS NULL
       OR TRUNC(p_end_date) < TRUNC(p_start_date) THEN
        RAISE_APPLICATION_ERROR(-20010, 'Enter a valid start date and end date.');
    END IF;

    SELECT NVL(SUM(amount), 0)
    INTO v_revenue
    FROM payment
    WHERE payment_status = 'PAID'
      AND payment_date >= TRUNC(p_start_date)
      AND payment_date < TRUNC(p_end_date) + 1;

    DBMS_OUTPUT.PUT_LINE('REPORT 2 - REVENUE');
    DBMS_OUTPUT.PUT_LINE('From ' || TO_CHAR(p_start_date, 'YYYY-MM-DD') ||
                         ' to ' || TO_CHAR(p_end_date, 'YYYY-MM-DD'));
    DBMS_OUTPUT.PUT_LINE('Total paid revenue: ' || TO_CHAR(v_revenue, 'FM999999990.00'));
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error in revenue report: ' || SQLERRM);
        RAISE;
END;
/

-- Report 3: one passenger's travel / booking history.
CREATE OR REPLACE PROCEDURE pr_passenger_travel_history (
    p_passenger_id IN NUMBER
)
IS
    v_passenger_name passenger.name%TYPE;
    v_rows NUMBER := 0;
BEGIN
    SELECT name
    INTO v_passenger_name
    FROM passenger
    WHERE passenger_id = p_passenger_id;

    DBMS_OUTPUT.PUT_LINE('REPORT 3 - TRAVEL HISTORY FOR ' || v_passenger_name);
    FOR rec IN (
        SELECT r.route_name, t.trip_date, t.departure_time,
               b.seat_no, b.price, b.status
        FROM booking b
        JOIN trip t ON t.trip_id = b.trip_id
        JOIN route r ON r.route_id = t.route_id
        WHERE b.passenger_id = p_passenger_id
        ORDER BY t.trip_date DESC, t.departure_time DESC
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(
            rec.route_name || ' | ' || TO_CHAR(rec.trip_date, 'YYYY-MM-DD') ||
            ' ' || rec.departure_time || ' | Seat ' || rec.seat_no ||
            ' | Price ' || TO_CHAR(rec.price, 'FM999999990.00') ||
            ' | ' || rec.status
        );
        v_rows := v_rows + 1;
    END LOOP;

    IF v_rows = 0 THEN
        DBMS_OUTPUT.PUT_LINE('No booking history was found for this passenger.');
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('Passenger ID was not found.');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error in travel-history report: ' || SQLERRM);
        RAISE;
END;
/

-- Report 4: vehicles with a service date that is due or overdue.
CREATE OR REPLACE PROCEDURE pr_vehicles_due_maintenance
IS
    v_rows NUMBER := 0;
BEGIN
    DBMS_OUTPUT.PUT_LINE('REPORT 4 - VEHICLES DUE FOR MAINTENANCE');
    FOR rec IN (
        SELECT v.registration_no, v.vehicle_type, m.maintenance_type,
               m.next_service_date, m.status
        FROM maintenance m
        JOIN vehicle v ON v.vehicle_id = m.vehicle_id
        WHERE m.next_service_date IS NOT NULL
          AND m.next_service_date <= TRUNC(SYSDATE)
          AND m.status <> 'COMPLETED'
        ORDER BY m.next_service_date
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(
            rec.registration_no || ' | ' || rec.vehicle_type || ' | ' ||
            rec.maintenance_type || ' | Due: ' ||
            TO_CHAR(rec.next_service_date, 'YYYY-MM-DD') || ' | ' || rec.status
        );
        v_rows := v_rows + 1;
    END LOOP;

    IF v_rows = 0 THEN
        DBMS_OUTPUT.PUT_LINE('No vehicles are currently due for maintenance.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error in maintenance report: ' || SQLERRM);
        RAISE;
END;
/

-- Report 5: trip occupancy based on confirmed/completed bookings.
CREATE OR REPLACE PROCEDURE pr_trip_occupancy_report
IS
    v_rows NUMBER := 0;
BEGIN
    DBMS_OUTPUT.PUT_LINE('REPORT 5 - TRIP BOOKING AND SEAT OCCUPANCY');
    FOR rec IN (
        SELECT t.trip_id, r.route_name, t.trip_date, t.departure_time,
               v.registration_no, v.seat_capacity,
               COUNT(b.booking_id) AS booked_seats,
               ROUND(COUNT(b.booking_id) * 100 / v.seat_capacity, 1) AS occupancy_pct
        FROM trip t
        JOIN route r ON r.route_id = t.route_id
        JOIN vehicle v ON v.vehicle_id = t.vehicle_id
        LEFT JOIN booking b
          ON b.trip_id = t.trip_id
         AND b.status IN ('CONFIRMED','COMPLETED')
        GROUP BY t.trip_id, r.route_name, t.trip_date, t.departure_time,
                 v.registration_no, v.seat_capacity
        ORDER BY t.trip_date, t.departure_time
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(
            'Trip ' || rec.trip_id || ' | ' || rec.route_name || ' | ' ||
            TO_CHAR(rec.trip_date, 'YYYY-MM-DD') || ' ' || rec.departure_time ||
            ' | Vehicle ' || rec.registration_no || ' | Booked ' || rec.booked_seats ||
            '/' || rec.seat_capacity || ' | Occupancy ' || rec.occupancy_pct || '%'
        );
        v_rows := v_rows + 1;
    END LOOP;

    IF v_rows = 0 THEN
        DBMS_OUTPUT.PUT_LINE('No trips were found.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error in trip occupancy report: ' || SQLERRM);
        RAISE;
END;
/

-- Check that the function and five report procedures compiled.
SELECT object_name, object_type, status
FROM user_objects
WHERE object_name IN (
    'FN_PASSENGER_TOTAL_PAID', 'PR_MOST_BOOKED_ROUTES',
    'PR_REVENUE_REPORT', 'PR_PASSENGER_TRAVEL_HISTORY',
    'PR_VEHICLES_DUE_MAINTENANCE', 'PR_TRIP_OCCUPANCY_REPORT'
)
ORDER BY object_type, object_name;

-- RUN REPORTS AFTER THE CREATION SCRIPT, ONE AT A TIME, AS NEEDED:
-- BEGIN pr_most_booked_routes; END;
-- /
-- BEGIN pr_revenue_report(DATE '2026-10-01', DATE '2026-10-31'); END;
-- /
-- DECLARE v_id NUMBER; BEGIN
--   SELECT passenger_id INTO v_id FROM passenger WHERE email = 'passenger1@smartmove.test';
--   pr_passenger_travel_history(v_id);
-- END;
-- /
-- BEGIN pr_vehicles_due_maintenance; END;
-- /
-- BEGIN pr_trip_occupancy_report; END;
-- /
-- Example of calling the function:
-- SELECT fn_passenger_total_paid(passenger_id) AS total_paid, name
-- FROM passenger ORDER BY name;
