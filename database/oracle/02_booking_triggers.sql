-- Optional business-rule triggers. Run as SMARTMOVE after 01_create_schema.sql.
-- These reserve a seat when a non-cancelled booking is created and restore it on cancellation.

CREATE OR REPLACE TRIGGER trg_booking_reserve_seat
BEFORE INSERT ON booking
FOR EACH ROW
DECLARE
    v_available NUMBER;
BEGIN
    IF UPPER(:NEW.status) <> 'CANCELLED' THEN
        SELECT available_seats
        INTO v_available
        FROM trip
        WHERE trip_id = :NEW.trip_id
        FOR UPDATE;

        IF v_available <= 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'No available seats remain for this trip.');
        END IF;

        UPDATE trip
        SET available_seats = available_seats - 1
        WHERE trip_id = :NEW.trip_id;
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20002, 'The selected trip does not exist.');
END;
/

CREATE OR REPLACE TRIGGER trg_booking_cancel_seat
BEFORE UPDATE OF status ON booking
FOR EACH ROW
DECLARE
    v_available NUMBER;
BEGIN
    IF UPPER(:OLD.status) <> 'CANCELLED' AND UPPER(:NEW.status) = 'CANCELLED' THEN
        UPDATE trip
        SET available_seats = available_seats + 1
        WHERE trip_id = :OLD.trip_id;
    ELSIF UPPER(:OLD.status) = 'CANCELLED' AND UPPER(:NEW.status) <> 'CANCELLED' THEN
        SELECT available_seats
        INTO v_available
        FROM trip
        WHERE trip_id = :OLD.trip_id
        FOR UPDATE;

        IF v_available <= 0 THEN
            RAISE_APPLICATION_ERROR(-20003, 'Cannot reactivate booking: no seats remain.');
        END IF;

        UPDATE trip
        SET available_seats = available_seats - 1
        WHERE trip_id = :OLD.trip_id;
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20004, 'The trip for this booking could not be found.');
END;
/

-- Check trigger status.
SELECT trigger_name, status
FROM user_triggers
WHERE trigger_name IN ('TRG_BOOKING_RESERVE_SEAT','TRG_BOOKING_CANCEL_SEAT')
ORDER BY trigger_name;
