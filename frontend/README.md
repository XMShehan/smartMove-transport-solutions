# SmartMove React frontend batch

This is a Vite + React user interface intended for the existing SmartMove Spring Boot API at `http://localhost:8080/api`.

## Run on macOS

1. Extract this ZIP. Finder creates `~/Downloads/SmartMove-React-Frontend`.
2. Keep Oracle and MongoDB containers running.
3. Keep the Spring Boot backend running in a separate Terminal window. Verify `http://localhost:8080/api/health/databases` first.
4. Open a NEW Terminal window and run:

```bash
cd ~/Downloads/SmartMove-React-Frontend
npm install
npm run dev
```

5. Open `http://localhost:5173` in your browser.

## Features

- Dashboard with live Oracle and MongoDB health and resource counts.
- List and create forms for managers, vehicles, drivers, routes, passengers, technicians, trips, bookings, payments, maintenance and Oracle feedback.
- Report views wired to the five backend report endpoints.
- MongoDB content lookup for reviews by route or keyword, highest-rated vehicles/drivers, vehicle documents, trip media and active announcements.
- Create forms for MongoDB review, announcement, vehicle-document metadata, and trip-media metadata.
- API loading and error feedback, responsive layout, and no database credentials in the frontend.

## Important notes

- The UI is a local development scaffold, not production-secure. The supplied backend batch has no authentication/authorization. Do not expose it on a public network.
- Vehicle-document and trip-media forms store metadata and a file path only; they do not upload files.
- The UI supports listing/creating records via available API endpoints. Edit/delete features for every entity are not present in the supplied backend batch.
- Use actual existing foreign-key IDs (manager, passenger, route, vehicle, driver, trip, technician) when creating records. Oracle constraints may reject invalid IDs.
- The frontend calls only the Java API. It does not connect directly to Oracle or MongoDB.
