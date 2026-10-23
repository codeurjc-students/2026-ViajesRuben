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
- Docker Compose files `docker/docker-compose.yml` (image `rubenruiz7/tripmate:0.1`) and `docker/docker-compose-dev.yml` (image `rubenruiz7/tripmate:dev`) with the application and MySQL, configured with environment variables that have defaults.
- Docker deployment documentation in the development guide.

### Changed

- Trip cards show the default cover image; the sample city photos were removed and will be served from the database.
- `GET /api/v1/trips` is paginated and returns a `PageDTO` instead of an array; the trip list loads 10 trips and a "Más resultados" button loads the rest.
- Frontend routing with React Router: one route per screen of the analysis (placeholders except `/trips`), the header and footer are now a shared layout, and the trip list moved from `/` to `/trips`.
- Frontend migrated from JavaScript to TypeScript (strict mode), with API types in `frontend/src/types/`.
- The backend now serves HTTPS instead of HTTP on port 8080: the Vite proxy, the client – server integration tests, the OpenAPI documentation and the Postman collection use `https://localhost`.
- The full quality workflow starts the backend on HTTPS port 8443, since the GitHub Actions runner cannot use port 443.

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
