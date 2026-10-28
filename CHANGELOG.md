# Changelog

All notable changes to the **TripMate** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Security base with Spring Security: login (`POST /api/v1/auth/login`), logout (`POST /api/v1/auth/logout`) and current user (`GET /api/v1/auth/me`) with a JWT stored in an `HttpOnly`, `Secure`, `SameSite=Strict` cookie.
- `User` entity with roles (`USER`, `ADMIN`) and status (`ACTIVE`, `BLOCKED`); blocked users cannot log in, and a user blocked after logging in is rejected on the next request.
- Single administrator defined in the configuration (`ADMIN_EMAIL`, `ADMIN_NAME`, `ADMIN_PASSWORD_HASH` with a BCrypt hash) and synchronized into the database on start-up.
- Access rules: `/api/v1/admin/**` requires the administrator, and the rest of `/api/**` requires a logged-in user, except login, logout and `GET /api/v1/trips`.
- Common JSON error body (`timestamp`, `status`, `error`, `message`, `path`) for validation and security errors.
- HTTPS on port 443 with a self-signed development keystore; port, keystore and token settings are configurable with environment variables.
- Tests for the security base: backend unit, integration and REST API system tests.
- `ResourceNotFoundException` (404) and `ConflictException` (409), and `GET /api/v1/trips/{id}` (404 JSON error for an unknown trip).
- Images stored in the database: `POST /api/v1/images` (logged-in users; JPEG, PNG or WebP up to `IMAGE_MAX_SIZE`, validated by type and file signature; 201 with `Location`) and public `GET /api/v1/images/{id}` with `Content-Type` and cache headers. `Image` ids are UUIDs.
- Frontend `ImageUpload` and `StoredImage` components and the `DEFAULT_TRIP_COVER` fallback image.
- Error handling for invalid images (400), too large uploads (413) and missing multipart parts (400).
- Pagination contract: `PageDTO` (`content`, `page`, `size`, `totalElements`, `last`), `?page=&size=` (10 per page by default, 50 at most) and the reusable `usePagedList` hook and "Más resultados" button in the frontend.
- Sample data now has 12 trips and is enabled with `SAMPLE_DATA` (`app.sample-data.enabled`); trips that are missing are added on start-up.
- Shared error page (same layout) for unknown routes (404), server errors (5xx) and no connection, with a button back to the home page.
- Error handling for malformed JSON and wrong parameter types (400), unsupported methods (405), unknown URLs (404) and unexpected errors (500, logged, without details in the response).
- The React application is served by Spring Boot over HTTPS: client routes such as `/trips` return `index.html` (also after a page refresh), unknown `/api/**` URLs keep returning the JSON 404 and missing files return 404.
- Docker image (`docker/Dockerfile`, multi-stage: Node build of the frontend, Maven build of the jar, JRE 21 runtime with a non-root user listening on port 443) and `.dockerignore`.
- Docker Compose files `docker/docker-compose.yml` (image `rubenruiz7/tripmate:0.1.0`) and `docker/docker-compose-dev.yml` (image `rubenruiz7/tripmate:dev`) with the application and MySQL, configured with environment variables that have defaults.
- Docker deployment documentation in the development guide.
- Continuous delivery with GitHub Actions: a reusable workflow builds and pushes the image `rubenruiz7/tripmate:<tag>` and publishes the Compose file as the OCI artifact `rubenruiz7/tripmate-compose:<tag>` to DockerHub. A push to `main` publishes `dev`, a published release `x.y.z` publishes `x.y.z` and `latest` (after checking that the commit is on `main` and passed Basic quality), and a manual run publishes `<branch>-<date>-<commit>`.
- Documentation of continuous delivery and versioning in the development guide, and instructions in the README to run the application from the OCI artifact.
- B2: user registration (`POST /api/v1/users`: 201 with `Location`, 400 for invalid data, 409 for a taken email or the administrator email; the new user is always a regular user and the password is stored hashed).
- B2: `/login` and `/register` pages with client-side validation and inline error messages, an authentication context (`/api/v1/auth/me` on load, login, logout, register), a header that shows the login and register links or the user name and "Cerrar sesión", and automatic login after registering.
- B2: private routes redirect anonymous visitors to `/login` and return them to the page they asked for after logging in.
- B2 tests: backend unit, integration and REST API tests, frontend unit and client – server integration tests, and a Selenium flow (register, log out, log in).
- B3: own profile in the API, only for the user themselves (`GET` and `PUT /api/v1/users/{id}`: 403 for another user, 404, 409 for a taken email or the administrator email). Changing the email does not log the user out, because the token identifies the user by id.
- B3: password change (`PUT /api/v1/users/{id}/password`), which requires the current password (400 if it is wrong) and follows the 8–72 character rule.
- B3: avatar stored in the database (`PUT`, `GET` and `DELETE /api/v1/users/{id}/avatar`), validated like any other image; replacing or deleting it deletes the previous image. Users without avatar show the initial of their name.
- B3: `/profile` page with view and edit mode for the name and email, a separate form to change the password, and the avatar with upload, replace and delete. The header shows the avatar and name, links to `/profile` and is updated without reloading.
- B3: the administrator's profile is read-only except for the avatar, because its details come from the configuration.
- B3 tests: backend unit, integration and REST API tests, frontend unit and client – server integration tests, and a Selenium flow (register, edit the name, change the avatar and the password, log out, log in with the new password).
- B4: every trip has a creator. Own trips can be created, read, edited and deleted (`POST /api/v1/trips` 201 with `Location`, `GET`, `PUT` and `DELETE /api/v1/trips/{id}`), and only the creator can use them: 403 for another user's trip (also for the administrator until trip moderation, B8), 404 for a missing one, 400 for invalid data (required name, destination and dates; the end date cannot be before the start date; optional budget and description with limits).
- B4: trip cover stored in the database (`PUT`, `GET` and `DELETE /api/v1/trips/{id}/cover`), validated like any other image; replacing or deleting it, or deleting the trip, deletes the previous image. Trips without cover show the default image.
- B4: `GET /api/v1/trips` can be sorted only by `id`, `name`, `destination`, `startDate`, `endDate` and `createdAt` (400 otherwise).
- B4: sample data now has three users (`ana@tripmate.com` with 12 trips, `carlos@tripmate.com` with 3 and `laura@tripmate.com` with 2, all with the password `Demo-2026`, which `SAMPLE_USERS_PASSWORD` can change), documented in the README and the development guide.
- B4: `/trips` ("Mis viajes") with "Nuevo viaje", `/trips/new`, `/trips/:id` and `/trips/:id/edit`. One form creates and edits a trip, with the cover chosen and previewed before saving; the trip page shows the cover and details and offers "Editar" and "Eliminar" to the creator; deleting asks for confirmation in a dialog; the list reloads after a deletion.
- B4: the confirmation dialog (`ConfirmModal`), the select-only mode of `ImageUpload`, the `useTrip` hook and `usePagedList.reload`; the error page explains 403 and 401.
- B4 tests: backend unit, integration and REST API tests (a negative case for every endpoint), frontend unit and client – server integration tests, and a Selenium flow as a sample user (create a trip with a cover, see it in the list, edit it, delete it).
- Known limitation documented: database schema changes need Flyway migrations after 0.1.0.
- B5: the activities of a trip's itinerary at `/api/v1/trips/{tripId}/activities`: `GET` (paginated, sorted by date and start time, filtered by day with `?date=yyyy-MM-dd`) and `POST` (201 with `Location`), and `GET`, `PUT` and `DELETE /api/v1/trips/{tripId}/activities/{activityId}`. Only the creator of the trip can use them (403 for another user, 404 for a missing trip, and 404 for an activity that belongs to another trip). An activity has a name, date, optional start and end times (the end strictly after the start), location, description, cost and category (`SIGHTSEEING`, `FOOD`, `TRANSPORT`, `ACCOMMODATION`, `LEISURE`, `OTHER`), and its date must be within the trip's dates (400).
- B5: the image of an activity stored in the database (`PUT`, `GET` and `DELETE /api/v1/trips/{tripId}/activities/{activityId}/image`), validated like any other image; replacing or deleting it, or deleting the activity, deletes the previous image.
- B5: deleting a trip now deletes its activities and their images first, in the same transaction.
- B5: the trip page shows the itinerary one day at a time: a tab per day of the trip with previous and next buttons (the strip scrolls on narrow screens), the activities of the selected day in time order on a vertical line, an empty state for days without activities, and "Nueva actividad" for the creator. The selected day is in the URL (`?day=`), so coming back from the form lands on the same day.
- B5: `/trips/:id/activities/new` and `/trips/:id/activities/:activityId/edit` with one form for both (category, day limited to the trip, start and end time, cost, location, description and an image chosen and previewed before saving), and a confirmation dialog to delete an activity.
- B5: sample data now has itineraries: Ana's "Lisbon getaway" (7 activities) and "Summer in Rome" (4), Carlos's "Andalusia route" (3) and Laura's "Weekend in Porto" (4, the third day left empty), some with generated images. A trip that already has activities is not touched.
- B5: `TripAccessService`, the single place that decides who can read or change a trip; `TripService` and `ActivityService` both use it, and the sample data, the activities and the later participants (I3) and administrator moderation (B8) extend it.
- B5 tests: backend unit, integration and REST API tests (a negative case for every endpoint, an activity of another trip, a date outside the trip, the filter by day, deleting a trip with its activities and images), frontend unit and client – server integration tests, and Selenium flows (browse the days, create, edit and delete an activity with an image, validation errors, the URL keeping the day, and the phone layout).
- B6: `GET /api/v1/trips/{tripId}/conflicts` (trip creator; 401, 403, 404) returns the warnings of the itinerary: activities of the same day whose times overlap (times that only touch do not, and activities without start and end time are ignored), activities outside the trip's dates, and the total cost when it is above the trip's budget (equal is fine, and a trip without budget has none). They are computed in the database queries and never block saving.
- B6: the trip page shows a warning above the itinerary, in Spanish, with the overlapping activities and their day, the activities outside the trip's dates with an "Editar" link, and the total cost against the budget; nothing is shown when there are no conflicts, and it is refreshed after an activity is deleted.
- B6 tests: backend unit, integration and REST API tests, frontend unit and client – server integration tests, and a Selenium flow (two overlapping activities show the warning, editing one removes it).
- `GET /api/v1/trips/{tripId}/activities/cost` (trip creator; 401, 403, 404) returns the total cost of the trip's activities, summed in the query, and the remaining budget (the budget minus that total, negative when over, `null` for a trip without budget); the trip page shows it below the budget as "Presupuesto restante" and refreshes it when an activity is deleted.
- B1: the public home page at `/`, in Spanish: a short introduction with "Regístrate gratis" and "Ya tengo cuenta" (a logged-in user sees "Ir a Mis viajes" instead), "Itinerarios de ejemplo" with the example trips three at a time (previous and next buttons appear only when there are more than three), and a "Cómo funciona" box with three steps. Each example card shows the cover, the name, the destination, the number of days and the budget, the first activity of its first three days and a "Ver itinerario completo" button.
- B1: `/examples/:tripId`, a read-only preview of an example trip: cover, trip data and the itinerary day by day, with no edit, delete or create buttons and no conflicts. A trip that is not an example is the 404 page.
- B1: the example trips are the trips the administrator created, so the administrator manages them with the normal trip and itinerary pages. `GET /api/v1/trips/examples` (paginated, each trip with the first activity of its first three days, read in one query for the whole page), `GET /api/v1/trips/examples/{id}` and `GET /api/v1/trips/examples/{id}/activities` (paginated, `?date=` like B5) are public and read-only; a trip the administrator did not create is a 404, never a 403.
- B1: the sample data gives the administrator three example trips (Rome, the Maldives and Tokyo) with a budget, a description, a cover photo from `backend/src/main/resources/sample_images/` and activities on every day of the trip. A sample trip that has no cover gets its photo when the application starts, so existing development databases get them too.
- B1 tests: backend unit, integration and REST API tests (anonymous list, preview and pagination, a trip of a regular user is a 404, the write endpoints stay 401), frontend unit and client – server integration tests, and a Selenium flow (an anonymous visitor opens `/`, sees the example trips, pages through them, opens one and browses its days).

### Changed

- B4: `GET /api/v1/trips` now returns only the logged-in user's trips (page, size and sort as before) and requires a login: it is no longer public. **A development database created before B4 must be recreated** (trips now have a mandatory creator, and `ddl-auto=update` cannot add it to existing rows); see the development guide.
- B4: `TripDTO` now has `creatorId`, `budget`, `description` and `coverImageId`; trip cards show the trip name, the destination and the dates, and the provisional participant count was removed (it returns with participants, I3).
- B4: the create trip button is now "Nuevo viaje", and `AvatarResult` became `ImageResult` (shared by the avatar and the cover).
- B4: the CI readiness check of the backend calls `/api/v1/auth/me`, since `/api/v1/trips` needs a login.
- The user returned by login, `GET /api/v1/auth/me` and the profile endpoints now includes `avatarId` and `registrationDate`.
- `ImageService` exposes `save` (returns the stored image for other domains to link) and `delete`; `ImageUpload` accepts an `upload` function and `StoredImage` a `fallback`.
- `TripCard` and `TripList` take their border and shadow from the `--tm-card-border` and `--tm-card-shadow` tokens (no visual change).
- Trip cards show the default cover image; the sample city photos were removed and will be served from the database.
- `GET /api/v1/trips` is paginated and returns a `PageDTO` instead of an array; the trip list loads 10 trips and a "Más resultados" button loads the rest.
- Frontend routing with React Router: one route per screen of the analysis (placeholders except `/trips`), the header and footer are now a shared layout, and the trip list moved from `/` to `/trips`.
- Frontend migrated from JavaScript to TypeScript (strict mode), with API types in `frontend/src/types/`.
- The backend now serves HTTPS instead of HTTP on port 8080: the Vite proxy, the client – server integration tests, the OpenAPI documentation and the Postman collection use `https://localhost`.
- The full quality workflow starts the backend on HTTPS port 8443, since the GitHub Actions runner cannot use port 443.
- Every screen except `/`, `/login`, `/register` and unknown URLs now requires a logged-in user in the frontend; the header no longer shows a placeholder user.
- Fixes for the issues reported by SonarQube Cloud: `ImageContentDTO` compares and prints its bytes by content, the `Allow` header of a 405 reads the supported methods once, props are read-only in the components, and the exception tests isolate the call that throws. The reason for disabling CSRF protection is recorded in the development guide.
- B5: `PUT /api/v1/trips/{id}` answers 409 when the new dates would leave an activity outside the trip; the activities have to be moved or deleted first. B6 will turn this into a warning.
- B5: the dialog that deletes a trip says that its activities go with it, and the trip form explains the 409 in Spanish.
- B5: `TripService` no longer checks the creator itself (`findTripForRead` and `findTripForWrite` moved to `TripAccessService`); the behaviour is the same.
- B5: `ImageService.deleteAll` removes several images in one statement, and the day tabs take their colors from the theme tokens.
- B6: `PUT /api/v1/trips/{id}` no longer answers 409 when the new dates leave an activity outside the trip; the change is saved and the activity is reported by `GET /api/v1/trips/{tripId}/conflicts`. Creating or editing an activity still requires a date inside the trip (400). The trip form no longer has a message for that 409.
- B1: only `GET /api/v1/trips/examples`, `GET /api/v1/trips/examples/{id}` and `GET /api/v1/trips/examples/{id}/activities` are public among the trip endpoints; everything else under `/api/` still needs a login.
- B1: `DataLoader` runs after the administrator account is created and needs `AdminProperties`; the home page `/` replaces the placeholder.
- B1: `ItinerarySection`, `DayActivities` and `useTrip` can read from another source (the example trips) and `ItinerarySection` has a read-only mode.

## [0.0.2] - 2026-10-19

### Added

- Spring Boot backend project (`backend/`) with Spring Web, Spring Data JPA and MySQL.
- `Trip` entity with its essential attributes (`name`, `destination`, `startDate`, `endDate`).
- Read-only REST API endpoint to list trips (`GET /api/trips`) using a `TripDTO`.
- Sample trip data loaded into the database on startup.
- OpenAPI documentation of the REST API (`docs/api/api-docs.yaml` and `docs/api/api-docs.html`).
- React frontend project (`frontend/`) built with Vite, showing the trip list on the home page.
- Backend unit tests with JUnit and Mockito.
- Backend integration tests with Testcontainers (MySQL).
- REST API system tests with Rest Assured.
- Frontend unit tests with Vitest and Testing Library.
- Client-server integration tests against the real REST API.
- UI system tests with Selenium.
- Test coverage reports with JaCoCo (backend) and Vitest V8 (frontend).
- Basic quality CI workflow (build, code style checks and unit tests on every push to feature branches).
- Full quality CI workflow (unit, integration and system tests on pull requests to `main`).
- Branch protection for `main` requiring pull requests and passing status checks.
- Code style checks with Spotless (backend), ESLint and Prettier (frontend).
- Postman collection with examples for all REST API operations.
- Development guide (`docs/development-guide.md`).

### Changed

- Documentation split into several files inside the `docs/` folder, with an index in `README.md`.
- `AI_USAGE.md` moved to `docs/AI_USAGE.md` and updated with the AI tools used in Phase 2.

## [0.0.1] - 2026-09-15

### Added

- Initial project documentation structure in `README.md`.
- Definition of functional and technical objectives for TFG Phase 1.
- Complete domain data model definition (`User`, `Trip`, `Participation`, `Invitation`, `Activity`, `AIProposal`).
- Role-based permissions matrix (Anonymous, Registered User, Trip Creator/Participant, Administrator).
- Specification of core features categorized by priority (Basic, Intermediate, Advanced).
- High-fidelity wireframe set designed in Figma for all 9 application screens.
- Navigation flow diagram and screen navigation matrix.
- Initial development methodology, project timeline, and Gantt chart.
- AI usage tracking documentation in `AI_USAGE.md`.

[Unreleased]: https://github.com/<org>/2026-ViajesRuben/compare/0.0.2...HEAD
[0.0.2]: https://github.com/<org>/2026-ViajesRuben/compare/0.0.1...0.0.2
[0.0.1]: https://github.com/<org>/2026-ViajesRuben/releases/tag/0.0.1
