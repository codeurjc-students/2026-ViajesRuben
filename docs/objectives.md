[← Back to README](../README.md)

# Objectives

## Functional objectives

The main functional objective is to provide a tool that lets a group of users organize a trip from start to finish without relying on spreadsheets or scattered chats, covering everything from registration and trip creation to collaborative itinerary building, AI-generated proposals, and a group chat per trip connected to the plan.

- Registration, login, and user profile management.
- Creation, viewing, editing, and deletion of trips (name, destination, dates, budget, description, cover image).
- Building a day-by-day itinerary through activities (location, date, schedule, cost, category, image).
- Inviting other users to a trip by email, with acceptance or rejection of the invitation.
- Collaboration between several participants on the same itinerary, with differentiated creator and participant roles.
- Automatic detection of conflicts in the plan (schedule overlaps, activities outside the trip dates, spending over budget).
- Generation of itinerary proposals via AI based on the group's preferences, budget, and characteristics, with manual review and acceptance before they are added.
- Visualization of trip spending through charts.
- A Telegram group for each trip, with invitations and notifications sent by a bot and access restricted to the trip's invited users.
- Administration panel for managing users, moderating trips, and viewing platform metrics.

## Technical objectives

From a technical standpoint, the application follows a Single Page Application (SPA) architecture: a frontend built with React and TypeScript that communicates through a REST API with a Spring Boot (Java) backend designed as a modular monolith, backed by a MySQL database, and packaged with Docker. It integrates external services (a language model for proposals, an email service, and the Telegram Bot API) and is built so it can later be split into independent services in TFG2. The selected optional parts are **static code analysis**, **AI integration**, and **integration with an instant messaging system**.

- SPA architecture: modular monolithic backend in Java with Spring Boot, organized by domain, exposing a REST API documented with OpenAPI, and a responsive SPA frontend in React with TypeScript that consumes it; designed for a future decomposition into services.
- Relational data model in MySQL with the entities User, Trip, Participation, Invitation, Activity, and AIProposal.
- Role-based authentication and authorization (anonymous, registered, administrator; creator/participant within each trip).
- Integration with a language model (LLM) via API to generate and reorganize itinerary proposals.
- Integration with the Telegram Bot API: account linking through deep links, one group per trip, invite links whose join requests are approved according to TripMate's permissions, and notifications about trip changes.
- Uploading and management of images associated with users, trips, and activities, stored in the database.
- Data visualization using a charting library on the frontend.
- Integration with an email-sending service to deliver invitations to trip participants.
- Implementation of a conflict-detection algorithm over the itinerary.
- Quality and delivery: automated tests at every level, continuous integration and delivery, static code analysis with Sonar, packaging with Docker and Docker Compose, and deployment.
