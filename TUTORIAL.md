# SimpleLibrary Tutorial — Building a Vaadin + jOOQ App with the AI Unified Process

This tutorial walks through building **SimpleLibrary**, a small book lending application,
from a blank canvas to a working, tested Vaadin + jOOQ app. It demonstrates the
[AI Unified Process](https://unifiedprocess.ai) (AIUP) end-to-end by chaining the skills
provided by the `aiup-core` and `aiup-vaadin-jooq` plugins.

Each step below corresponds to one slash command you run inside Claude Code. The output
of each step is a concrete artifact (a markdown file, a diagram, a SQL script, Java code,
or a test class) that the next step builds on.

> **Prerequisites**
>
> - Java 25 (the project ships `mvnw`)
> - Docker (or Testcontainers Cloud) for the PostgreSQL container
> - Claude Code with the `aiup-core` and `aiup-vaadin-jooq` plugins installed
> - The starter project from this repository, freshly cloned

## Why a process at all?

AI agents are great at producing code, but without structure they tend to improvise.
AIUP fixes that by giving the AI a **predefined chain of artifacts** to produce, each one
narrowing the design space for the next. By the time you reach `/implement`, the AI has a
vision, a use case spec, an entity model, and a database schema to anchor its output —
so the code it writes is grounded in your domain, not in a generic template.

The flow we follow:

```
vision.md → /requirements → /entity-model → /use-case-diagram → /use-case-spec
   → /flyway-migration → /implement → /browserless-test → /plawright-test
```

## Step 1 — Review the Vision

The vision is the only artifact you write by hand. Everything else is generated from it.
For this tutorial the vision is already prepared at [`docs/vision.md`](docs/vision.md) —
open it and read it before continuing. It is the source of truth for every step that
follows.

The vision describes the **problem**, the **solution**, the **target users**, the
**goals**, and the **non-goals**. Keep it short — one page is plenty. If you adapt this
tutorial to your own project, that is the structure to follow.

> **Tip.** Be explicit about non-goals. Calling out what is **out of scope** (no payments,
> no reservations, no self service sign-up) tells the AI not to scaffold features you do
> not need yet.

> **Pre-seeded security.** This starter project already ships Spring Security, an
> `app_user` table (identity only), a login view, and two seeded accounts (see the
> [README](README.md#pre-seeded-security)). The tutorial therefore does **not** cover
> authentication — your generated views just need to declare `@RolesAllowed("MEMBER")`,
> `@RolesAllowed("LIBRARIAN")`, or both. Treat `app_user` as a given; the `member` table
> is a *domain* entity that the tutorial creates and links to `app_user` via a
> `user_id` foreign key.

## Step 2 — Generate the Requirements Catalog

```
/requirements
```

This skill reads `docs/vision.md` and produces a structured requirements catalog:

- **Functional requirements** as user stories (e.g. *As a member, I want to search the
  catalog so that I can find a book quickly*).
- **Non-functional requirements** (performance, usability, maintainability).
- **Constraints** (the tech stack from the README, the "no authentication" non-goal,
  the "no multi-branch" non-goal, …).

The catalog is written to `docs/requirements.md`. Review it before continuing — this is
your last chance to course-correct cheaply. If a story is missing or a non-goal slipped
in as a requirement, fix it now. Every later step compounds on this list.

## Step 3 — Build the Entity Model

```
/entity-model
```

From the requirements, the skill identifies the **domain entities** and produces a
Mermaid ER diagram in `docs/architecture/entity-model.md`, including:

- Entities (`Book`, `Member`, `Loan`)
- Attributes with types
- Relationships and cardinalities
- Validation rules (required fields, lengths, uniqueness)

For SimpleLibrary you should expect roughly:

- `Book` (id, title, author, isbn, copies)
- `Member` (id, user_id, name, email) — a library patron, linked 1:1 to an existing
  `app_user` row via `user_id`
- `Loan` (id, book_id, member_id, borrowed_at, returned_at)

The `AppUser` entity (id, username, password_hash, role) is part of the starter and
does **not** need to appear in the entity model — treat it as an external boundary.
Verify the cardinalities — a `Loan` should reference exactly one `Book` and one
`Member`, and a `Member` can have many `Loan`s.

## Step 4 — Draw the Use Case Diagram

```
/use-case-diagram
```

The skill renders a PlantUML use case diagram from the requirements at
`docs/architecture/use-case-diagram.puml`. It shows the two actors (**Member**,
**Librarian**) and the use cases they participate in:

- UC-001 Search catalog
- UC-002 Borrow book
- UC-003 Return book
- UC-004 View active loans (Librarian)
- UC-005 Manage catalog (Librarian)

The exact numbering depends on what the requirements step produced. Use the diagram to
confirm coverage: every requirement should land on at least one use case, and every use
case should trace back to a requirement.

## Step 5 — Write the First Use Case Specification

```
/use-case-spec UC-001
```

Pick the use case that **unblocks the others** to go first. For SimpleLibrary that is
**UC-001 Search catalog** — it has no preconditions, gives the member something to do
the moment they land on the page, and produces the data structures that UC-002 (Borrow
book) needs.

The skill writes `docs/use_cases/UC-001-search-catalog.md` with:

- Actors and stakeholders
- Preconditions / postconditions
- Main success flow (numbered steps)
- Alternative flows (no results, partial match)
- Acceptance criteria

This spec is the contract for the rest of the chain. Keep it concrete: a step like *"the
system shows the books"* is not enough — *"the system shows a Grid with columns Title,
Author, ISBN, and Available Copies, sorted by Title ascending"* is.

> Repeat this step once per use case as you grow the app. The tutorial implements UC-001
> end-to-end first; you can come back for UC-002 and UC-003 later using the same chain.

## Step 6 — Generate the Flyway Migration

```
/flyway-migration
```

The skill reads the entity model and the use case spec and emits a versioned Flyway
script under `src/main/resources/db/migration/`. The starter already contains
`V001__create_app_user.sql`, so the skill should produce a new version (e.g.
`V002__create_member_book_loan.sql`) for the missing tables. It includes:

- `CREATE TABLE` statements for `member`, `book`, and `loan`
- Primary and foreign keys, including `member.user_id → app_user.id` and
  `loan.member_id → member.id`
- `NOT NULL` constraints driven by the validation rules in the entity model
- Useful indexes (e.g. on `loan.book_id`, `loan.member_id`)
- Seed data that links the existing `alice` `app_user` row to a freshly inserted
  `member` row so the seeded login can borrow a book end-to-end

Run `./mvnw compile` once after this step — it will start a Testcontainers Postgres,
apply the migration, and regenerate the jOOQ metamodel so the next step has typed
references to your new tables.

## Step 7 — Implement the Use Case

```
/implement UC-001
```

This is where the chain pays off. The skill reads the use case spec, the entity model,
and the generated jOOQ classes, and produces:

- A jOOQ-based **repository** (`BookRepository`) in the `domain` package
- A **service** layer if the flow needs business logic
- A Vaadin **view** (`SearchCatalogView`) in the `ui` package, wired to the navigation
  in `MainLayout`
- DTOs / records for the data passed between layers

The generated code follows the project conventions you saw in the README: feature
packages split into `ui` and `domain`, jOOQ for data access, no Spring Data, no JPA.

Run the app via `TestApplication` from your IDE and click through the flow. You should
be able to type a query, see the Grid filter, and see the results sorted as the spec
requires.

## Step 8 — Write Browserless UI Tests

```
/browserless-test UC-001
```

The skill creates a UI unit test under `src/test/java/.../SearchCatalogViewTest.java`
that extends `AbstractBrowserlessTest`. It exercises the view without a browser by
driving the Vaadin component tree directly:

- Navigate to the view
- Look up the search field and the grid
- Type a query, assert the grid shows the expected rows
- Cover the alternative flows from the spec (empty result, etc.)

Run it with `./mvnw test`. These tests are fast — they should run in under a second per
test — so they form the inner feedback loop while you iterate on the view.

## Step 9 — Write the End-to-End Test

```
/playwright-test UC-001
```

The final skill produces a Playwright + Mopo E2E test under
`src/test/java/.../SearchCatalogIT.java` extending `PlaywrightIT`. It boots the full
Spring Boot app on a random port and drives a real browser through the use case:

- Start the app with a fresh database
- Open the catalog page
- Type a search query
- Assert the visible rows match what the use case spec requires

Run with `./mvnw verify` to execute the integration tests. This is your outer feedback
loop — slower than the browserless tests, but it proves that the JavaScript, CSS,
routing, and database layers all line up.

## What you have at the end

After running the full chain for UC-001 you have:

- A documented vision and requirements catalog
- An entity model and use case diagram covering the whole app
- A use case spec, a database migration, an implementation, and two layers of tests for
  the first use case
- A repeatable workflow for adding the next use case (UC-002 Borrow book, UC-003 Return
  book, …)

To add a new use case, you only need steps **5, 7, 8, 9** — the requirements, entity
model, and diagram cover the whole app, so they do not need to be regenerated. Use cases
that need new tables or columns will also re-trigger step **6** (with a new `V002__…`
migration).

## Where to go next

- Run the chain again for UC-002 Borrow book — it will reuse `Book` and `Member` and
  add rows to `Loan`.
- Continue with UC-003 Return book to close the lifecycle.
- Add the librarian-facing UC-004 View active loans for the operational view.
- Once a few use cases exist, explore the `aiup-vaadin-jooq` plugin's testing skills to
  improve coverage and to migrate any legacy tests you bring in.

The full methodology and the catalog of skills are documented at
[unifiedprocess.ai](https://unifiedprocess.ai).
