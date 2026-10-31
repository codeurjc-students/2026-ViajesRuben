# AI Usage Declaration

This document records the transparent and responsible use of Artificial Intelligence (AI)
tools throughout the development of the undergraduate thesis project **TripMate**, in
accordance with the academic integrity guidelines of the Universidad Rey Juan Carlos (URJC)
and the TFG development guide.

All AI-generated output has been read, understood, reviewed and adapted by the student, who
is fully responsible for the delivered work.

## Log of AI tool usage by phase

### Phase 1: Definition of features, screens and analysis

#### Record 1.1 — Conceptual brainstorming and documentation formatting

- **Date:** 15/09/2026
- **Phase:** Phase 1 (Definition of features, screens and analysis)
- **Goal:** Brainstorm initial project concepts and get minor help with the Markdown formatting of `README.md`.
- **Tool:** Google Gemini
- **Specific version:** Gemini 1.5 Pro
- **Tool configuration:** Web chat interface, conversational mode.
- **How it was used:**
  - Used during early ideation sessions to explore feature scope and domain entity ideas.
  - Used to review and polish the Markdown layout and structure of `README.md`.
- **Tool complements (plugins, skills, MCP servers, tools):** None.
- **Context files:** None.
- **Spec-Driven Development files:** None.
- **Human validation:** All ideas, project decisions, data model, wireframes and written content were evaluated, drafted and finalized by the student. The AI tool was only a minor aid for brainstorming and formatting.

### Phase 2: Repository, tests and CI

#### Record 2.1 — Phase 2 planning

- **Date:** 28/09/2026
- **Phase:** Phase 2 (Repository, tests and CI)
- **Goal:** Turn the Phase 2 requirements of the TFG guide into a planning.
- **Tool:** Claude (Anthropic)
- **Specific version:** Claude Opus 5.5
- **Tool configuration:** claude.ai web chat, conversational (chat) mode, default settings.
- **How it was used:** The Phase 2 section of the TFG guide (PDF) was attached, and the AI was asked for a planning covering project setup, automated tests, CI and documentation.
- **Tool complements:** None. Only a file attachment (TFG guide PDF) was used.
- **Context files:** None.
- **Spec-Driven Development files:** None.
- **Human validation:** The student reviewed the plan against the TFG guide and the Phase 2 tracking rubric.

#### Record 2.2 — Fixing test compilation errors

- **Date:** 11/10/2026
- **Phase:** Phase 2 (Repository, tests and CI)
- **Goal:** Fix compilation errors when running `mvnw test` on Windows.
- **Tool:** Claude (Anthropic)
- **Specific version:** Claude Opus 5.5
- **Tool configuration:** claude.ai web chat, conversational (chat) mode, default settings.
- **How it was used:** The Maven console output was pasted, and the AI was asked for the possible causes. The AI identified some problems:
  - The test files lacked `package` and `import` statements.
  - `TestcontainersConfiguration` had to be made `public`.
- **Tool complements:** None.
- **Context files:** None.
- **Spec-Driven Development files:** None.
- **Human validation:** The student applied the fixes and re-ran the tests until they compiled.

#### Record 2.3 — Diagnosing a NullPointerException in the REST API system test

- **Date:** 13/10/2026
- **Phase:** Phase 2 (Repository, tests and CI)
- **Goal:** Find the cause of a `NullPointerException` in `TripApiE2ETest`.
- **Tool:** Claude (Anthropic)
- **Specific version:** Claude Opus 5.5
- **Tool configuration:** claude.ai web chat, conversational (chat) mode, default settings.
- **How it was used:** The Surefire report stack trace was pasted. The AI identified an incompatibility between Rest Assured 5.x and Groovy 5 (included in Spring Boot 4), and suggested upgrading Rest Assured to 6.x.
- **Tool complements:** None.
- **Context files:** None.
- **Spec-Driven Development files:** None.
- **Human validation:** The student upgraded the dependency, verified the resolved version and confirmed the test passed.

#### Record 2.4 — Screenshots required by the documentation

- **Date:** 14/10/2026
- **Phase:** Phase 2 (Repository, tests and CI)
- **Goal:** Identify which screenshots the TFG guide requires for the documentation.
- **Tool:** Claude (Anthropic)
- **Specific version:** Claude Opus 5.5
- **Tool configuration:** claude.ai web chat, conversational (chat) mode, default settings.
- **How it was used:** The AI was asked which screenshots should be taken according to the guide. It separated the mandatory ones (test execution and coverage) from the recommended ones (CI runs, board, application).
- **Tool complements:** None.
- **Context files:** None.
- **Spec-Driven Development files:** None.
- **Human validation:** The student checked the list against the documentation section of the TFG guide.

#### Record 2.5 — CHANGELOG update

- **Date:** 16/10/2026
- **Phase:** Phase 2 (Repository, tests and CI)
- **Goal:** Update `CHANGELOG.md` with the Phase 2 changes.
- **Tool:** Claude (Anthropic)
- **Specific version:** Claude Opus 5.5
- **Tool configuration:** claude.ai web chat, conversational (chat) mode, default settings.
- **How it was used:** The existing `CHANGELOG.md` was pasted, and a detailed text of all the changes donde during phase 2 was given. Then the AI was asked to update it.
- **Tool complements:** None.
- **Context files:** None.
- **Spec-Driven Development files:** None.
- **Human validation:** The student reviewed nothing was missing in the new CHANGELOG and verified the information containing was correct.

#### Record 2.6 — AI usage declaration update

- **Date:** 16/10/2026
- **Phase:** Phase 2 (Repository, tests and CI)
- **Goal:** Make this document comply with the fields required by the TFG guide and add missing information.
- **Tool:** Claude (Anthropic)
- **Specific version:** Claude Opus 5.5
- **Tool configuration:** claude.ai web chat, conversational (chat) mode, default settings.
- **How it was used:** The existing `AI_USAGE.md` was pasted, and the AI was asked to review it and compare with all the required fields, as well as adding missing information.
- **Tool complements:** None.
- **Context files:** None.
- **Spec-Driven Development files:** None.
- **Human validation:** The student reviewed every record against what was actually done.

### Phase 3: Basic functionality (B1–B8)

#### Record 3.1 — Security base (login, roles, blocked users, configured administrator and HTTPS)

- **Date:** 20/10/2026
- **Phase:** Phase 3 (Basic functionality, B1–B8)
- **Goal:** Consult specific Spring Security 6 configurations and review minor technical details regarding JWT handling in cookies and SSL keystore setup during manual implementation. Also, helped with documentation of development-guide.md based on all the changes and contributions.
- **Tool:** Claude Code (Anthropic CLI).
- **Specific version:** Claude Sonnet 5.5.
- **Tool configuration:** Terminal CLI used strictly for quick interactive Q&A sessions.
- **How it was used:**
  - **Syntax & Concept Queries:** Consulted the tool for quick syntax checks regarding Spring Security 6 filter chain configuration and JWT cookie extraction parameters.
  - **Documentation Editing:** Used the assistant to review and refine the wording of manual changelog entries, OpenAPI endpoint descriptions and development-guide.md writing.
- **Tool complements (plugins, skills, MCP servers, tools):** None. Standard interactive terminal interface.
- **Context files:** None utilized.
- **Spec-Driven Development files:** Personal technical notes maintained locally.
- **Human validation:** The student reviewed and verified all AI responses and suggestions before manually integrating them into the project. The correct integration was subsequently validated through the execution of unit, integration, and REST system tests, as well as manual checks against the running application.

#### Record 3.2 — Package-by-domain refactor and API v1

- **Date:** 21/10/2026
- **Phase:** Phase 3 (Basic version)
- **Goal:** Move the backend to `/api/v1` and reorganise it by domain (`trip`, `common`, …).
- **Tool:** Claude Code (Anthropic)
- **Specific Version:** Claude Code <version>; model Sonnet 5.5
- **Tool Configuration:** VS Code extension on Windows; model alias `opusplan`
- **How it was used:**
  - The agent proposed the package structure (`com.ruben.backend.trip` with controller/dto/entity/repository/service, plus `common`) and moved the classes and tests.
  - I chose package-by-domain over package-by-layer after comparing both options.
- **Tool Complements (MCPs, Plugins, etc.):** review subagent
- **Context Files:** private Claude Code configuration; README analysis
- **Human Validation:** I ran the full test suite and checked that CI passed before merging. PR: <link>

#### Record 3.3 — Frontend migration to TypeScript

- **Date:** 22/10/2026
- **Phase:** Phase 3 (Basic version)
- **Goal:** Migrate the React frontend from JavaScript to TypeScript.
- **Tool:** Claude Code (Anthropic)
- **Specific Version:** Claude Code <version>; models Opus 5.5 / Sonnet 5.5
- **Tool Configuration:** VS Code extension on Windows; model alias `opusplan`
- **How it was used:**
  - The agent renamed the components and tests to `.tsx`, added types (`src/types`) and the TypeScript configuration, and updated the Vitest configs.
- **Tool Complements (MCPs, Plugins, etc.):** custom skills
- **Context Files:** private Claude Code configuration
- **Human Validation:** I ran `npx tsc --noEmit`, lint, unit and integration tests, reviewed the code and checked the app in the browser. PR: <link>

#### Record 3.4 — Configuration through environment variables

- **Date:** 22/10/2026
- **Phase:** Phase 3 (Basic version)
- **Goal:** Make the database URL, credentials and sample data configurable through environment variables.
- **Tool:** Claude Code (Anthropic) and chat
- **Specific Version:** Claude Code <version>; model Opus 5.5
- **Tool Configuration:** VS Code extension on Windows; `opusplan`
- **How it was used:**
  - The agent replaced fixed values with `${VAR:default}` placeholders (`DB_URL`, `SAMPLE_DATA`, …).
  - The chat assistant pointed out an inconsistency between database names (`tripmate` vs `tripplanner`) and MySQL versions (9 vs 8.4); I decided to use `tripmate` and `mysql:9` everywhere (local, Testcontainers and CI).
- **Tool Complements (MCPs, Plugins, etc.):** private configuration
- **Context Files:** private Claude Code configuration
- **Human Validation:** I recreated the local MySQL container and ran all tests. PR: <link>

#### Record 3.5 — Docker image and Docker Compose deployment

- **Date:** 23/10/2026
- **Phase:** Phase 3 (Basic functionality, B1–B8)
- **Goal:** Package TripMate as a single Docker image served by Spring Boot over HTTPS and run it with Docker Compose and MySQL, following section 5.4 of the TFG guide.
- **Tool:** Claude Code (Anthropic)
- **Specific Version:** Claude Code 2.1.309; models Opus 5.5 (planning) / Sonnet 5.5 (implementation)
- **Tool Configuration:** Terminal CLI on Windows; effort medium
- **How it was used:**
  - The AI proposed a plan in four stages, then generated the SPA forwarding in Spring (`SpaResourceResolver`, `SpaWebConfig`) with its unit and REST API system tests, the multi-stage `Dockerfile`, the `.dockerignore`, the two Compose files and the Docker section of the development guide, README and CHANGELOG. It also ran the builds, the tests and the Docker checks and reported their output.
  - The student rejected the proposed port 8443 inside the container and required port 443, with the non-root user kept through a sysctl (root as fallback), and asked for that decision to be documented in "Design decisions".
- **Tool Complements (MCPs, Plugins, etc.):** custom skills, review subagents
- **Context Files:** private Claude Code configuration kept outside a spec-driven plan for `docker`
- **Human Validation:** The student reviewed the plan before approving it and every stage before committing it. The result was validated with the unit and REST API system tests of the new classes, the full backend unit test suite, the image build, and manual checks of `docker-compose-dev.yml` (database healthy before the application, API, SPA routes, administrator login, empty database with `SAMPLE_DATA=false`, data kept after `down` and `up`, and removed with `down -v`).

#### Record 3.6 — Continuous delivery to DockerHub

- **Date:** 23/10/2026
- **Phase:** Phase 3 (Basic functionality, B1–B8)
- **Goal:** Publish the Docker image and the Compose file as an OCI artifact to DockerHub with GitHub Actions for `main`, releases and manual runs, and document continuous delivery and versioning.
- **Tool:** Claude Code (Anthropic)
- **Specific Version:** Claude Code 2.1.309; models Opus 5.5 (planning) / Sonnet 5.5 (implementation)
- **Tool Configuration:** Terminal CLI on Windows; effort low.
- **How it was used:**
  - The AI proposed a plan in two stages and answered the questions asked in the request (how the artifact references the image tag, the Compose version, the release gate, the documentation). It then generated the reusable workflow and the three caller workflows, the sanitizing of the branch name, the release gate, and the "Continuous delivery" and "Versioning" sections of the development guide, the README instructions, the CHANGELOG entries and the Compose tag change.
  - The student rejected the first plan and required changes: releases publish only `x.y.z` and `latest` (never `x.y`) with the full version in `docker/docker-compose.yml` and in the release steps; every `docker compose -f oci://…` command must be non-interactive; the release gate must wait for a running Basic quality run instead of failing; and the guide must state that the gate assumes branch protection on `main`.
- **Tool Complements (MCPs, Plugins, etc.):** custom skills, review subagents; MCP none
- **Context Files:** private Claude Code configuration kept outside the public repository (project instructions, path-scoped rules, skills, subagents, hooks); a spec-driven plan for `continuous-delivery`, also kept private
- **Human Validation:** The student reviewed and changed the plan before approving it and reviewed each stage before committing it. The AI's checks were `actionlint` on all the workflows, the branch tag conversion with several branch names, the Compose rewrite for the release, dev and manual tags (only the image line changes), `docker compose publish` and reading the artifact back from a local registry, and `prettier --check` on the documentation. The workflows themselves can only run on GitHub once they are on `main`, so their first real runs (`dev` after the merge, a manual run, and the 0.1.0 release) are still to be verified by the student. PR: <link>

#### Record 3.7 — Fixing the SonarQube Cloud issues

- **Date:** 24/10/2026
- **Phase:** Phase 3 (Basic functionality, B1–B8)
- **Goal:** Fix the open SonarQube Cloud issues of `main` (quality gate failing on reliability and security ratings) without changing the behaviour of the application.
- **Tool:** Claude Code (Anthropic)
- **Specific Version:** Claude Code; models Opus 5.5
- **Tool Configuration:** Terminal CLI; effort low.
- **How it was used:**
  - The AI was used to document the adjustments and fixes carried out, as well as to review the codebase and SonarQube analysis to detect whether any additional fixes remained to be done.
  - The AI classified `java:S4502` (CSRF disabled) as a false positive and left it to the student to mark in SonarQube; it did not change the configuration. It left `java:S1135` (a `TODO` for B1/B4) unfixed for the same reason.
- **Tool Complements (MCPs, Plugins, etc.):** Custom skills, review subagents
- **Context Files:** A plan for the issue fixes, and a list of the issues reported
- **Human Validation:** The student reviewed and changed the plan before approving it. The AI's checks were the backend unit tests (100 passed, coverage 91.2% of instructions), the integration and REST API system tests (62 passed), `TripUiE2ETest` against the backend and the frontend preview (2 passed), the frontend unit tests (75 passed), `spotless:check`, `eslint`, `tsc` and `prettier --check`.

#### Record 3.8 — User registration and login (B2)

- **Date:** 24/10/2026
- **Phase:** Phase 3 (Basic functionality, B1–B8)
- **Goal:** Implement feature B2: account registration and the login and register screens on top of the existing security base.
- **Tool:** Claude Code (Anthropic)
- **Specific Version:** Claude Code 2.1.310; models Opus 5.5 (planning) / Sonnet 5.5 (implementation)
- **Tool Configuration:** Terminal CLI on Windows; effort medium; modes plan → manual approval of each step
- **How it was used:**
  - The AI wrote the plan, the registration endpoint (`POST /api/v1/users`), the login and register pages, the authentication context and route guard and the six test types. Two review subagents checked the tests and the diff against the project rules.
  - The student changed the plan before approving it (a generic message for 400 errors, and `TripUiE2ETest` logging in as a regular user instead of the administrator) and decided the password length (8–72 characters), that the `TripCard`/`TripList` CSS cleanup is left for later, and that redirecting to `/login` when the API is down is acceptable.
- **Tool Complements (MCPs, Plugins, etc.):** custom skills, review subagents and hooks (private configuration); MCP none
- **Context Files:** Claude Code configuration; a spec-driven plan for `B2`.
- **Human Validation:** The student reviewed and changed the plan before approving it and will review the diff before opening the PR. The AI's checks were the backend suite (180 passed, unit coverage 91.2%), the frontend tests (127 passed, coverage 99.6%), the client – server integration tests (10 passed) and the Selenium tests (7 passed) against the current code. PR: <link>

#### Record 3.9 — Own profile, password and avatar (B3)

- **Date:** 24/10/2026
- **Phase:** Phase 3 (Basic functionality, B1–B8)
- **Goal:** Implement feature B3: view and edit one's own profile (name, email, password, avatar) on top of B2 and the existing image-in-database code.
- **Tool:** Claude Code (Anthropic)
- **Specific Version:** Claude Code; models Opus 5.5 and Sonnet 5.5
- **Tool Configuration:** Terminal CLI on Windows; plan mode first, then five stages, each with its tests, its output.
- **How it was used:**
  - The AI explored the code, wrote the plan, and implemented the profile, password and avatar endpoints, the `/profile` page, the avatar in the header and the six test types. It also regenerated the OpenAPI documentation, updated the Postman collection, the development guide and the changelog. Two review subagents checked the tests and the diff against the project rules.
  - The student decided that the administrator's profile is read-only except the avatar, that a wrong current password is a 400, and asked for the design decisions and the known limitation (a password change does not close other sessions) to be written in the development guide before the implementation. The student made every commit.
- **Tool Complements (MCPs, Plugins, etc.):** Custom skills, review subagents; MCP none
- **Context Files:** Claude Code configuration; a spec-driven plan for `B3`.
- **Human Validation:** The student reviewed and amended the plan before approving it and reviewed and committed each stage.

#### Record 3.10 — Trips: create, view, edit and delete (B4)

- **Date:** 25/10/2026
- **Phase:** Phase 3 (Basic functionality, B1–B8)
- **Goal:** Implement feature B4: create, view, edit and delete one's own trips, including a cover image, and set the patterns for B5–B8.
- **Tool:** Claude Code (Anthropic)
- **Specific Version:** Claude Code; models Opus 5.5 (planning) / Sonnet 5.5 (implementation)
- **Tool Configuration:** Terminal CLI on Windows; effort high; modes plan → manual approval of each stage
- **How it was used:**
  - Used as a support agent to draft the initial plan for `B4`, assist with the code implementation across five stages (endpoints, UI components, tests), and update project documentation (OpenAPI, Postman, README, changelog).
  - The student guided the development by defining architectural criteria (validation sequence, database recreation strategy, frontend text localization), setting strict plan requirements, requesting UI refinements, and reviewing and making every commit.
- **Tool Complements (MCPs, Plugins, etc.):** Custom skills, review subagents, and hooks; MCP none.
- **Context Files:** A spec-driven plan for `B4`.
- **Human Validation:** Full human oversight throughout: the student reviewed and approved the plan, guided adjustments, and verified each stage before committing.

#### Record 3.11 — Activities of a trip's itinerary (B5)

- **Date:** 26/10/2026
- **Phase:** Phase 3 (Basic functionality, B1–B8)
- **Goal:** Help implement feature B5.
- **Tool:** Claude Code (Anthropic)
- **Specific Version:** Claude Code; model Sonnet 5.5 (planning and implementation)
- **Tool Configuration:** Terminal CLI on Windows; modes plan → manual approval of each stage
- **How it was used:**
  - Used as a support agent to draft the plan for `B5`, and to write some parts of the code and tests across five stages, and the documentation (development guide, changelog). The review subagents audited the tests and the code against the guide rules.
  - The student approved the plan and changed its design. The student also tried the application by hand.
- **Tool Complements (MCPs, Plugins, etc.):** Custom skills, review subagents, and hooks.
- **Context Files:** Private Claude Code configuration and a spec-driven plan for `B5`.
- **Human Validation:** The student reviewed and approved the plan, decided the design change, tried the application manually and verified each stage before committing.

#### Record 3.12 — Conflict detection in the itinerary (B6)

- **Date:** 27/10/2026
- **Phase:** Phase 3 (Basic functionality, B1–B8)
- **Goal:** Help implement feature B6.
- **Tool:** Claude Code (Anthropic)
- **Specific Version:** Claude Code 2.1.311; models Opus 5.5 (planning) / Sonnet 5.5 (implementation)
- **Tool Configuration:** Terminal CLI on Windows; modes plan → manual approval of each stage
- **How it was used:**
  - Used as a support agent to draft the plan for `B6`, and to write some parts of the code and tests across three stages, and the documentation (OpenAPI, Postman collection, development guide, changelog). The review subagents audited the tests and the code against the guide rules.
- **Tool Complements (MCPs, Plugins, etc.):** Custom skills and review subagents.
- **Context Files:** Some Claude Code configuration and a spec-driven plan for `B6`.
- **Human Validation:** The student reviewed and approved the plan, tried the application manually and verified each stage.

#### Record 3.13 — Public home page with example itineraries (B1)

- **Date:** 28/10/2026
- **Phase:** Phase 3 (Basic functionality, B1–B8)
- **Goal:** Help implement feature B1.
- **Tool:** Claude Code (Anthropic)
- **Specific Version:** Claude Code; models Opus 5.5 (planning) / Sonnet 5.5 (implementation)
- **Tool Configuration:** Terminal CLI on Windows; modes plan → manual approval of each stage
- **How it was used:**
  - Used as a support agent to draft the plan for `B1`, and to write some parts of the code and tests across four stages (public endpoints, sample data, home page and preview, documentation), and the documentation (OpenAPI, Postman collection, development guide, changelog).
- **Tool Complements (MCPs, Plugins, etc.):** Custom skills, review subagents and hooks.
- **Context Files:** Some Claude Code configuration and a spec-driven plan for `B1`.
- **Human Validation:** The student reviewed and approved the plan, made the necessary changes, chose the photos and verified each stage before committing.

#### Record 3.14 — User management by the administrator (B7)

- **Date:** 31/10/2026
- **Phase:** Phase 3 (Basic functionality, B1–B8)
- **Goal:** Help implement feature B7.
- **Tool:** Claude Code (Anthropic)
- **Specific Version:** Claude Code 2.1.316; models Opus 5.5 (planning) / Sonnet 5.5 (implementation)
- **Tool Configuration:** Terminal CLI on Windows; modes plan → manual approval of each stage
- **How it was used:**
  - Used as a support agent to draft the plan for `B7`, and to write some parts of the code and tests across five stages, and the documentation (OpenAPI, Postman collection, development guide, changelog). The review subagents audited the tests and the diff against the project rules.
  - The student approved the plan, set the commands to run the UI tests (preview on port 5174) and to format the code before each commit, and changed or rejected some of the generated code during review.
- **Tool Complements (MCPs, Plugins, etc.):** Custom skills, review subagents and hooks.
- **Context Files:** Some Claude Code configuration and a spec-driven plan for `B7`.
- **Human Validation:** The student reviewed and approved the plan and verified each stage before committing.
