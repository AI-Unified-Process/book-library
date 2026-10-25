# Book Library Tutorial — Building a Web App with the AI Unified Process

This tutorial walks through building the **Book Library**, a small book lending application, from
a blank canvas to a tested Vaadin + jOOQ app. It demonstrates the
[AI Unified Process](https://unifiedprocess.ai) end-to-end by chaining the skills of `aiup-core`
and the layer plugins for Flyway, jOOQ, Spring Boot, and Vaadin in Claude Code. The same
walkthrough is on the web at [unifiedprocess.ai/tutorial.html](https://unifiedprocess.ai/tutorial.html).

Each step corresponds to one slash command and produces a concrete artifact (a markdown spec, a
diagram, an architecture document, a SQL script, Java code, or a test class) that the next step
builds on.

> **Prerequisites**
>
> - Java 25 (the project ships `mvnw`)
> - Docker (or Testcontainers Cloud) for the PostgreSQL container
> - Claude Code with `aiup-core` and the layer plugins of the Book Library stack:
>
>   ```
>   /plugin marketplace add ai-unified-process/marketplace
>   /plugin install aiup-core
>   /plugin install aiup-flyway
>   /plugin install aiup-jooq
>   /plugin install aiup-spring-boot
>   /plugin install aiup-vaadin
>   ```
>
> - This starter project, freshly cloned
>
> Still on the combined `aiup-vaadin-jooq` plugin? It is deprecated and will be removed with
> aiup-core 4.0.0; see [Layer plugins](https://unifiedprocess.ai/layer-plugins.html).

## Why a process at all?

AI agents are great at producing code, but without structure they tend to improvise. The AI
Unified Process gives the AI a **predefined chain of artifacts** to produce, each one narrowing
the design space for the next. By the time you reach `/use-case-implement`, the AI has a vision,
a use case spec, an entity model, and a stack file that names the skill for every layer, so the
code it writes is grounded in your domain, not in a generic template.

```
vision.md → /requirements → /entity-model
  → /use-case-diagram → /use-case-spec → /spec-review
  → /stack → /use-case-implement
  → /use-case-test → /use-case-coverage
```

## Step 1 — Review the Vision

The vision is the only artifact you write by hand. Everything else is generated from it. For this
tutorial it is already prepared at [`docs/vision.md`](docs/vision.md); read it before continuing.
Keep a vision short — one page: the problem, the solution, the target users, the goals, and the
non-goals.

> **Tip.** Be explicit about non-goals. Calling out what is **out of scope** (no payments, no
> reservations, no self service sign-up) tells the AI not to scaffold features you do not need yet.

> **Pre-seeded security.** The starter ships Spring Security, an `app_user` table (identity only),
> a login view, and two seeded accounts (`librarian` / `librarian`, `alice` / `alice`; see the
> [README](README.md#pre-seeded-security)). The tutorial therefore does **not** cover
> authentication; your generated views just need `@RolesAllowed("MEMBER")` for the member-only views
> (borrowing, returning, my loans: a librarian has no `member` row and does not borrow, as
> [`docs/architecture.md`](docs/architecture.md) records), `@RolesAllowed("LIBRARIAN")` for the
> librarian-only ones, and `@RolesAllowed({"MEMBER", "LIBRARIAN"})` for the catalog search both use.

## Step 2 — Generate the Requirements Catalog

```
/requirements
```

The skill reads `docs/vision.md`, asks up to five clarifying questions where the vision leaves a
decision open, and writes the requirements catalog to `docs/requirements.md` and the domain terms
to `docs/glossary.md`:

- **Functional requirements** as user stories (*As a member, I want to search the catalog so that
  I can find a book without asking the librarian*).
- **Non-functional requirements** (performance, usability, maintainability), each measurable.
- **Constraints** (the tech stack, the non-goals from the vision, …).

Review the catalog before continuing; this is your last chance to course-correct cheaply.

## Step 3 — Build the Entity Model

```
/entity-model
```

From the requirements the skill writes `docs/entity_model.md`: a Mermaid ER diagram of the
entities and their relationships, plus one attribute table per entity with data types, lengths
or precision, and validation rules. For the Book Library expect roughly:

- `Book` (id, title, author, isbn, copies)
- `Member` (id, user_id, name, email) — a library member, linked 1:1 to an existing `app_user` row
- `Loan` (id, book_id, member_id, borrowed_at, returned_at)

The `AppUser` entity (id, username, password_hash, role, created_at) is part of the starter and does not need
its own section in the entity model; treat it as an external boundary that `member.user_id`
references.

## Step 4 — Draw the Use Case Diagram

```
/use-case-diagram
```

The skill writes a PlantUML use case diagram to `docs/use_cases.puml` with the two actors
(**Member**, **Librarian**) and their use cases:

- UC-001 Search catalog
- UC-002 Borrow book
- UC-003 Return book
- UC-004 View active loans (Librarian)
- UC-005 Manage catalog (Librarian)
- UC-006 Manage members (Librarian)
- UC-007 View my loans

Every requirement should land on at least one use case, and every use case should trace back to
a requirement.

## Step 5 — Write the First Use Case Specification

```
/use-case-spec UC-001
```

Pick the use case that **unblocks the others** first: UC-001 Search catalog has no preconditions
and produces the data structures UC-002 needs. The skill writes
`docs/use_cases/UC-001-search-catalog.md` with the actors, the trigger, the requirements it links,
preconditions, the main success scenario, alternative flows, success and failure postconditions,
and business rules (BR-001, …). It starts with `Status: Draft`.

> **Be concrete.** "The system shows the books" is not enough; "the system shows the books with
> the columns Title, Author, ISBN, and Available Copies, sorted by title ascending" is. Name what
> the actor sees, not the widget: `/spec-review` flags a Grid or a button colour as the wrong level.

> **Review before you implement.** Run `/spec-review UC-001` once the spec reads well; name the use
> case, because without an id the review covers the whole project and reports every use case of the
> diagram that has no specification yet as an error. It checks
> that every reference resolves and that the use case agrees with the requirements, the entity
> model, and the other use cases. It only reports; fix the findings with `/use-case-spec` and
> review again. Once the review is clean, set the Status line to `Approved`.

## Step 6 — Record the Architecture

```
/stack
```

The skill detects Flyway, jOOQ, Spring Boot, and Vaadin from the `pom.xml` and the source tree
and writes `docs/architecture/stack.md`: the Layers table (migration `/flyway-schema`,
persistence `/jooq-persistence`, service `/spring-service`, UI `/vaadin-flow-ui`, E2E
`/vaadin-e2e-test`, plus the test skill of each layer), the integration layer (UI: the
Browserless tests of the view run in process against the real database), the E2E scope, the
module layout, and the commands.

The skill detects the layers but not how many browser tests you want, so it asks one question: the
E2E scope. Answer *Use cases and test cases*; steps 8 and 9 rely on it. With *Test cases only*,
`/use-case-test` writes no `UC001SearchCatalogIT`, and step 9 has to name the layer explicitly.

> **Entry layers.** Every Book Library use case is offered through the UI, so the specifications need
> no `**Layers:**` line. In an app that also has a REST API, a use case offered through only one of
> them says so with a line such as `**Layers:** UI`.

The starter's [`docs/architecture.md`](docs/architecture.md) records the package-by-feature layout
and that every feature has a service; `/stack` reads it as input and leaves it where it is.

## Step 7 — Implement the Use Case

```
/use-case-implement UC-001
```

The skill reads the use case, the requirements it links, the entity model, and the stack file,
asks about gaps in the specification in one round, then runs the layer skills bottom-up:

- `/flyway-schema` adds `V002__…sql` next to the starter's `V001__create_app_user.sql`, with the
  tables, keys (including `member.user_id → app_user.id`), and constraints.
- `/jooq-persistence` writes `BookRepository` against the generated jOOQ classes, projecting into
  Java records.
- `/spring-service` writes `BookService` with the transaction boundary and the business rules.
- `/vaadin-flow-ui` writes `SearchCatalogView`, wired to `MainLayout` and calling the service.

It writes no tests. Run the app via `TestApplication` from your IDE, or with
`./mvnw spring-boot:test-run` on the command line, sign in as `alice` / `alice`, and open the
catalog. It is still empty: no use case adds books yet, and the production migrations
hold no demo data, so you see the empty-state message the spec names. The books arrive with the
test data of the next step.

## Step 8 — Write the Use Case Tests

```
/use-case-test UC-001
```

The skill tests the use case once, in the integration layer. With Vaadin Flow that is the UI, so
`/vaadin-flow-ui-test` writes `UC001SearchCatalogTest` extending `AbstractBrowserlessTest`. It
drives the view without a browser, but against the real service and database, and covers every
alternative flow and business rule. Each test method carries `@UseCase(id = "UC-001")`; the test
data goes into a repeatable migration, `src/test/resources/db/migration/R__uc001_search_catalog.sql`,
which Flyway picks up next to the production migrations without any configuration. Once that test
data fills the catalog, the empty-catalog flow cannot be arranged with it, so its test method is
annotated `@Transactional`, deletes the books inside the test transaction, and checks the
empty-catalog message; the rollback restores the test data. With the E2E scope *Use cases and test cases*, the same run
then ends with the E2E layer and writes the end-to-end test of step 9 as well.

Because these tests run through every layer below, the repository and the service get no tests of
their own. Run them with `./mvnw test`. They are fast, under a second per test, so they form the
inner feedback loop while you iterate on the view. Then start `TestApplication` again (or `./mvnw spring-boot:test-run`): it loads the same test data,
so now you can type a query, see the grid filter, and see the results sorted as the spec requires.

## Step 9 — Write the End-to-End Test

```
/use-case-test UC-001 e2e
```

With the E2E scope *Use cases and test cases*, the run from step 8 already ends with the E2E
layer; naming it writes or reconciles only this one. `/vaadin-e2e-test` produces a Playwright +
Drama Finder test, `UC001SearchCatalogIT`, extending `AbstractBasePlaywrightIT`. It boots the app
on a random port, signs in through the login view, and drives a real browser through the use case.
The empty-catalog flow has no browser test: the running app has one data set, and the Browserless
test already covers it.

Run it with `./mvnw verify`. This is your outer feedback loop: slower than the Browserless tests,
but it proves that the JavaScript, CSS, routing, and database layers all line up.

## What you have at the end

A documented vision and requirements catalog, an entity model and use case diagram covering the
whole app, a stack file of the layers, and for UC-001 a spec, a migration, an implementation across
four layers, Browserless tests in the integration layer, and an end-to-end test.

- **Add the next use case:** steps 5, 7, 8, 9. When it needs new tables or columns,
  `/use-case-implement` adds a new `V003__…` migration as its first layer.
- **Change an existing use case:** edit the specification, then re-run steps 7, 8, and 9; the skills
  reconcile the existing code and tests instead of adding second ones. Never edit a migration that
  has already been applied.

## Where to go next

- Run `/use-case-coverage UC-001` to audit what you built against its specification. Every NFR and
  constraint on the use case's `**Requirements:**` line is a unit too: a limit a test can observe,
  such as a response time, stays open until a test asserts it, and only then does the audit suggest
  `Tested`. Link only the NFRs the use case has to prove; one that holds for the whole system, such as
  sign-in, needs no link.
- Run the chain again for UC-002 Borrow book; it reuses `Book` and `Member` and adds rows to `Loan`.
  The seeded account `alice` has no `member` row yet, so its test data links a member of its own; to
  borrow as alice in the running app, give her one in the test data or with UC-006 Manage members.
- Continue with UC-003 Return book and the librarian-facing UC-004 View active loans.
- Once a few use cases exist, run `/test-case UC-001 UC-002 UC-003` to write a TC-001 journey,
  automate it with `/test-case-implement TC-001`, and audit it with `/test-case-coverage TC-001`.

The full methodology and the catalog of skills are documented at
[unifiedprocess.ai](https://unifiedprocess.ai).
