# SmartMove Transport Solutions — Simple Query Pack

This pack is an annotated learning scaffold based on the latest supplied Data Management 2 CW brief and the user's latest ER/schema diagrams. The CW brief states that AI-generated content receives 0 marks, so do not submit these files unchanged. Study them, test them, adapt them to your team's design, and follow your lecturer's rules.

## Files

- `oracle/01_create_schema.sql` — creates the 11 Oracle tables and relational constraints.
- `oracle/02_booking_triggers.sql` — reserves a seat for a non-cancelled booking and restores it when a booking is cancelled/reactivated.
- `oracle/03_sample_data.sql` — fictional sample records for demonstrating operations and reports.
- `oracle/04_basic_queries.sql` — common SELECT/join/report queries and commented CRUD practice templates.
- `oracle/05_plsql_reports.sql` — one function and five PL/SQL report procedures with cursor loops and exception handling.
- `mongodb/06_mongodb_setup_and_queries.js` — MongoDB collections, sample documents, indexes and the four CW query scenarios.

## Oracle run order

Use Oracle SQL Developer and connect as `SMARTMOVE` (not `SYSTEM`). Open each `.sql` file and use **Run Script (F5)**. Run files in this order:

1. `oracle/01_create_schema.sql` — run only once on an empty SMARTMOVE schema.
2. `oracle/02_booking_triggers.sql` — run after the tables exist.
3. `oracle/03_sample_data.sql` — run once after the triggers exist; it inserts fictional demo data and commits it.
4. `oracle/04_basic_queries.sql` — run a query at a time with Cmd+Enter, or F5 for all SELECTs. Keep the commented DML examples commented until you edit them.
5. `oracle/05_plsql_reports.sql` — use F5 / Run Script so the `/` terminators and `SET SERVEROUTPUT ON` are processed.

The sample data uses predictable fictional emails such as `passenger1@smartmove.test`; the exact generated numeric IDs are determined by Oracle. Find actual IDs by querying email/registration number instead of assuming ID numbers in your application.

If a DDL script partially succeeds and then stops with an error, do not run it repeatedly without checking which tables/objects were created. Query `USER_TABLES` and `USER_ERRORS` first. Do not drop tables or volumes unless you intentionally want to remove data.

## MongoDB run

The environment report showed MongoDB is running in the Docker container `smartmove-mongodb`, and the host has no `mongosh` command. From the extracted `SmartMove-CW-Query-Pack` folder, copy the script into the container:

```bash
docker cp mongodb/06_mongodb_setup_and_queries.js smartmove-mongodb:/tmp/06_mongodb_setup_and_queries.js
```

Then run it using the configured root account (type the password when prompted):

```bash
docker exec -it smartmove-mongodb mongosh -u smartmove_root -p --authenticationDatabase admin /tmp/06_mongodb_setup_and_queries.js
```

If the container was started with a different MongoDB root username, use the matching username from your private `.env` file. Never paste passwords into source files or commit `.env` to GitHub.

The sample MongoDB documents use example Oracle reference IDs to demonstrate query structure. In the integrated application, Spring Boot must validate the real Oracle IDs before saving corresponding MongoDB documents. The Oracle and MongoDB databases do not automatically enforce foreign keys across databases.

## Design assumptions to confirm

- `TRIP.trip_date` is in addition to `departure_time` and `arrival_time`.
- `ROUTE.duration_time` is measured in minutes.
- `PAYMENT.booking_id` is unique, enforcing the chosen 1:1 Booking–Payment relationship.
- `manager_id` appears in the same tables as the latest schema diagram: Vehicle, Driver, Route, Passenger, Technician, Payment, Maintenance and Feedback.
- Password columns are examples only. In the Java backend, store salted password hashes produced by a modern password-hashing library, never plain-text passwords.
- The schema uses identity columns for IDs. The business validation in a real app should also check seat availability, the trip/vehicle's capacity, trip status, valid booking status transitions, and the payment status.
