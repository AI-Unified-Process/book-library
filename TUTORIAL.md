# SimpleLibrary Tutorial — Building a Vaadin + jOOQ App with the AI Unified Process

This tutorial walks through building **SimpleLibrary**, a small book lending application,
from a blank canvas to a working, tested Vaadin + jOOQ app. It demonstrates the
[AI Unified Process](https://unifiedprocess.ai) end-to-end by chaining the skills
provided by `aiup-core` and the layer plugins `aiup-flyway`, `aiup-jooq`,
`aiup-spring-boot`, and `aiup-vaadin`.

Each step below corresponds to one slash command you run inside Claude Code. The output
of each step is a concrete artifact (a markdown file, a diagram, a SQL script, Java code,
or a test class) that the next step builds on.

> **Prerequisites**
>
> - Java 25 (the project ships `mvnw`)
> - Docker (or Testcontainers Cloud) for the PostgreSQL container
> - Claude Code with `aiup-core` and the layer plugins of this stack installed (see below)
> - The starter project from this repository, freshly cloned

SimpleLibrary combines Flyway, jOOQ, Spring Boot, and Vaadin Flow, so it needs one plugin
per technical layer next to `aiup-core`:

```
/plugin marketplace add ai-unified-process/marketplace
/plugin install aiup-core
/plugin install aiup-flyway
/plugin install aiup-jooq
/plugin install aiup-spring-boot
/plugin install aiup-vaadin
```

> **Still on `aiup-vaadin-jooq`?** The combined plugin is deprecated and will be removed
> with `aiup-core` 4.0.0. [Layer plugins](https://unifiedprocess.ai/layer-plugins.html#moving-an-existing-project)
> explains the switch and maps the old commands to the new ones. Do not install both in
> one project.

## Why a process at all?

AI agents are great at producing code, but without structure they tend to improvise.
The AI Unified Process fixes that by giving the AI a **predefined chain of artifacts** to
produce, each one narrowing the design space for the next. By the time you reach
`/implement-uc`, the AI has a vision, a use case spec, an entity model, and a recorded
architecture to anchor its output —
so the code it writes is grounded in your domain, not in a generic template.

The flow we follow:

```
vision.md → /requirements → /entity-model → /use-case-diagram → /use-case-spec
   → /architecture → /implement-uc → /test-uc → /coverage-uc
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

## Step 6 — Record the Architecture

```
/architecture
```

Before the first line of code, the skill records which layer skill builds and tests each
technical layer. It detects Flyway, jOOQ, Spring Boot, and Vaadin from the `pom.xml` and the
source tree of the starter and writes the **Development View** to
`docs/architecture/development.md`, together with `docs/architecture/README.md`, an index of
the 4+1 architecture views. The Development View holds:

- **The Layers table** — migration with `/flyway-schema`, persistence with
  `/jooq-persistence`, service with `/spring-service`, UI with `/vaadin-flow-ui`, the test
  skill of each layer that has one, and the E2E layer with `/vaadin-e2e-test`
- **The integration layer** — UI: the topmost layer whose tests run in process against the
  real database, which with Vaadin Flow are the Browserless tests of the view
- **The module layout and commands** — where the code of each layer lives, and how to build
  and test it

The starter already ships a `docs/architecture.md` with its package conventions; the skill
reads it as input and asks whether to move it into the folder as the Development View, so
the project keeps one architecture document. You run this step once per project, and again
only when a layer changes. Review the file
like any other artifact: the next three steps read it.

> **Entry layers.** Every SimpleLibrary use case is offered through the UI, so the
> specifications need no `**Layers:**` line. In an app that also has a REST API, a use case
> offered through only one of them says so with a line such as `**Layers:** UI`.

## Step 7 — Implement the Use Case

```
/implement-uc UC-001
```

This is where the chain pays off. The skill reads the use case spec, the requirements it
links, the entity model, and the Development View, asks about any gaps in the specification
in one round, then runs the layer skills bottom-up. Each layer hands the types and methods
it created over to the next:

- **`/flyway-schema`** writes a versioned Flyway script under
  `src/main/resources/db/migration/`. The starter already contains
  `V001__create_app_user.sql`, so it adds a new version (e.g.
  `V002__create_member_book_loan.sql`) with the tables, primary and foreign keys (including
  `member.user_id → app_user.id`), and the `NOT NULL`, `UNIQUE`, and `CHECK` constraints
  from the entity model.
- **`/jooq-persistence`** writes a jOOQ-based repository (`BookRepository`) against the
  classes jOOQ generates from the migration, with the results projected into Java records.
- **`/spring-service`** writes the service with the transaction boundaries and the business
  rules of the use case.
- **`/vaadin-flow-ui`** writes the Vaadin view (`SearchCatalogView`), wired to `MainLayout`
  and calling the service directly.

The skill runs the build in between, so the jOOQ classes are current before the repository
is written. It writes no tests. The generated code follows the project conventions you saw
in the README: feature packages split into `ui` and `domain`, jOOQ for data access, no
Spring Data, no JPA.

Run the app via `TestApplication` from your IDE and click through the flow. You should be
able to type a query, see the Grid filter, and see the results sorted as the spec requires.

## Step 8 — Write the Use Case Tests

```
/test-uc UC-001
```

The skill tests the use case once, in the integration layer. With Vaadin Flow that is the
UI, so `/vaadin-flow-ui-test` writes `UC001SearchCatalogTest` under `src/test/java`,
extending `SpringBrowserlessTest`. It drives the view without a browser, but against the
real service and database:

- Navigate to the view
- Look up the search field and the grid
- Type a query, assert the grid shows the expected rows
- Cover every alternative flow and business rule from the spec (empty result, etc.)

Each test method carries `@UseCase(id = "UC-001")`, and the test data goes into a
repeatable migration under `src/test/resources/db/testdata/`. Because these tests run
through every layer below, the repository and the service get no tests of their own.

Run them with `./mvnw test`. They are fast — under a second per test — so they form the
inner feedback loop while you iterate on the view.

## Step 9 — Write the End-to-End Test

```
/test-uc UC-001 e2e
```

The `/test-uc` run from step 8 ends with the E2E layer, so the test is usually already
there; naming the layer writes or reconciles only this one. `/vaadin-e2e-test` produces a
Playwright + Drama Finder test, `UC001SearchCatalogIT`, extending
`AbstractBasePlaywrightIT`. It boots the full Spring Boot app on a random port and drives a
real browser through the use case:

- Start the app with a fresh database
- Open the catalog page
- Type a search query
- Assert the visible rows match what the use case spec requires

Run with `./mvnw verify` to execute the integration tests. This is your outer feedback
loop — slower than the browserless tests, but it proves that the JavaScript, CSS, routing,
and database layers all line up.

## What you have at the end

After running the full chain for UC-001 you have:

- A documented vision and requirements catalog
- An entity model and use case diagram covering the whole app
- A Development View that records the layers and the skills that build and test them
- A use case spec, a database migration, an implementation across four layers,
  Browserless tests in the integration layer, and an end-to-end test for the first use case
- A repeatable workflow for adding the next use case (UC-002 Borrow book, UC-003 Return
  book, …)

To add a new use case, you only need steps **5, 7, 8, 9** — the requirements, entity
model, diagram, and Development View cover the whole app, so they do not need to be
redone. When a use case needs new tables or columns, `/implement-uc` adds a new `V003__…`
migration as its first layer.

To change an existing use case — a new feature, a change request, or a bug — edit the
specification, then re-run steps 7, 8, and 9. `/implement-uc` finds the existing view,
service, and repository and reconciles them with what the spec now requires, and `/test-uc`
updates the existing test classes rather than adding second ones. A change that needs a new
column gets a new migration; never edit one that has already been applied.

## Where to go next

- Run `/coverage-uc UC-001` to audit what you just built against its specification: it
  lists which scenario steps, alternative flows, and business rules have code and tests
  behind them, and which are still open.
- Run the chain again for UC-002 Borrow book — it will reuse `Book` and `Member` and
  add rows to `Loan`.
- Continue with UC-003 Return book to close the lifecycle.
- Add the librarian-facing UC-004 View active loans for the operational view.
- Chain several use cases into one journey with `/test-case`, then automate it with
  `/test-uc TC-001`.
- Read [Layer plugins](https://unifiedprocess.ai/layer-plugins.html) for how the layers
  combine and what the four construction commands own.

The full methodology and the catalog of skills are documented at
[unifiedprocess.ai](https://unifiedprocess.ai).
