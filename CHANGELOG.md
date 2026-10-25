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
- B4: sample data now has three users (`ana@tripmate.com` with 12 trips, `carlos@tripmate.com` with 3 and `laura@tripmate.com` with 2, all with the password `Demo-2026`), documented in the README and the development guide.
- B4: `/trips` ("Mis viajes") with "Nuevo viaje", `/trips/new`, `/trips/:id` and `/trips/:id/edit`. One form creates and edits a trip, with the cover chosen and previewed before saving; the trip page shows the cover and details and offers "Editar" and "Eliminar" to the creator; deleting asks for confirmation in a dialog; the list reloads after a deletion.
- B4: the confirmation dialog (`ConfirmModal`), the select-only mode of `ImageUpload`, the `useTrip` hook and `usePagedList.reload`; the error page explains 403 and 401.
- B4 tests: backend unit, integration and REST API tests (a negative case for every endpoint), frontend unit and client – server integration tests, and a Selenium flow as a sample user (create a trip with a cover, see it in the list, edit it, delete it).
- Known limitation documented: database schema changes need Flyway migrations after 0.1.0.

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
