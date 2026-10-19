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
