# Book Library

The **Book Library** is the reference example project for the
[AI Unified Process](https://unifiedprocess.ai) tutorial. It is built step by step throughout
the tutorial to demonstrate how to drive a real Vaadin and jOOQ application from requirements
to running code with the help of AI-assisted, disciplined engineering.

## About the AI Unified Process

The [AI Unified Process](https://unifiedprocess.ai) (AIUP) is a lightweight, AI-assisted
software process that combines proven practices from the Unified Process with modern AI tooling.
Rather than letting an AI agent improvise, AIUP guides it through clearly defined artifacts:

1. **Requirements catalog** – functional requirements (user stories), non-functional requirements,
   and constraints.
2. **Use case specifications** – detailed scenarios with main and alternative flows.
3. **Use case diagram** – PlantUML overview of actors and system boundaries.
4. **Entity model** – Mermaid ER diagram with attributes and validation rules.
5. **Flyway migrations** – versioned SQL scripts derived from the entity model.
6. **Implementation** – Vaadin views and jOOQ data access generated from the use cases.
7. **Tests** – Browserless UI unit tests and Playwright end-to-end tests.

Each step has a dedicated skill in the `aiup-core` and `aiup-vaadin-jooq` plugins. The tutorial
walks through using them in order to grow the Book Library from a blank canvas into a working
application.

See [TUTORIAL.md](TUTORIAL.md) for the step-by-step walkthrough that builds **SimpleLibrary** on
top of this starter project using the AIUP skills end-to-end.

## Technology Stack

- [Vaadin Flow](https://vaadin.com) for the server-side UI
- [jOOQ](https://jooq.org) for type-safe SQL and the data access layer
- [Spring Boot](https://spring.io/projects/spring-boot/) as the application framework
- [PostgreSQL](https://www.postgresql.org) as the database
- [Flyway](https://flywaydb.org) for schema migrations
- [Testcontainers](https://testcontainers.com) for jOOQ code generation and integration tests
- [Vaadin Browserless Testing](https://vaadin.com/docs/latest/flow/testing/browserless) for UI unit tests
- [Playwright](https://playwright.dev) with [Mopo](https://github.com/viritin/mopo) for E2E tests
- [ArchUnit](https://www.archunit.org) for architectural rules

## Running the Application

Before running the application, the jOOQ metamodel has to be generated using the Maven plugin:

    ./mvnw compile

Then you can run the application with a database started by Testcontainers from your IDE using the `TestApplication`.

**Important:**
This class uses the [Spring Boot Testcontainers support](https://spring.io/blog/2023/06/23/improved-testcontainers-support-in-spring-boot-3-1/), introduced with Spring Boot 3.1.
Thus, [Docker](https://www.docker.com) or [Testcontainers Cloud](https://testcontainers.com/cloud/) must be running on your local computer.

## Pre-seeded Security

Security is wired up out of the box so the tutorial can focus on business features. The first
migration (`V001__create_app_user.sql`) creates the `app_user` table — the identity table
used purely for authentication. Domain entities like `member` live in their own tables and
link to `app_user` via a foreign key, which the tutorial introduces.

`SecuritySeed` inserts two accounts on first start:

| Username    | Password    | Role       |
|-------------|-------------|------------|
| `librarian` | `librarian` | LIBRARIAN  |
| `alice`     | `alice`     | MEMBER     |

Every routable view must declare its access rule with `@RolesAllowed("MEMBER")`,
`@RolesAllowed("LIBRARIAN")`, or `@RolesAllowed({"MEMBER", "LIBRARIAN"})`. Use
`@AnonymousAllowed` only on the login view. The current user is available via the
`CurrentUser` Spring bean (`core/security/CurrentUser.java`), which exposes the
`app_user.id`. Features that own a domain entity linked to `app_user` (e.g. `member`)
look it up through that id. The architecture and rationale are documented in
[`docs/architecture.md`](docs/architecture.md).

## Testing the Application

There are two base classes:

- `AbstractBrowserlessTest` can be used for fast [browserless testing](https://vaadin.com/docs/latest/flow/testing/browserless), aka UI unit test. It extends Vaadin's `SpringBrowserlessTest`, which sets up a Vaadin mock environment without a browser.
- `PlaywrightIT` configures Playwright for E2E tests. This class uses SpringBootTest at a random port.

The Playwright test uses [Mopo](https://github.com/viritin/mopo), which simplifies the testing of Vaadin applications with Playwright.

## Deploying to Production

To create a production build, call `mvnw clean package -Pproduction` (Windows),
or `./mvnw clean package -Pproduction` (Mac & Linux).

This will build a JAR file with all the dependencies and front-end resources, ready to be deployed. You can find
the file in the `target` folder after the build completes.

Once the JAR file is built, you can run it using `java -jar target/book-library-<version>.jar`

## Project Structure

- `core/ui/layout/MainLayout.java` contains the navigation setup using [App Layout](https://vaadin.com/docs/components/app-layout).
- `core` package holds cross-cutting concerns: configuration, security, shared UI components.
- Feature packages (e.g. `greeting`) follow a `ui` / `domain` split so each use case stays self-contained.
- `src/main/resources/db/migration` contains the Flyway migrations that drive jOOQ code generation.
- `src/main/resources/META-INF/resources` contains the custom CSS styles.

## Useful Links

### AI Unified Process

- Visit [unifiedprocess.ai](https://unifiedprocess.ai) for the methodology and tutorial.

### Vaadin

- Check out the [Vaadin Developer Portal](https://vaadin.com/developers).
- Read the documentation at [vaadin.com/docs](https://vaadin.com/docs).
- Create new projects at [start.vaadin.com](https://start.vaadin.com/).
- Find a collection of solutions to common use cases at [cookbook.vaadin.com](https://cookbook.vaadin.com/).
- Find add-ons at [vaadin.com/directory](https://vaadin.com/directory).

### jOOQ

- Read the documentation at [jooq.org/learn](https://www.jooq.org/learn/).
- Browse the [Blog](https://blog.jooq.org).

### Spring Boot

- Explore the [Spring Boot project page](https://spring.io/projects/spring-boot/).

### Testcontainers

- Go to the [Testcontainers website](https://testcontainers.com).

### Vaadin Browserless Testing

- Read the [documentation](https://vaadin.com/docs/latest/flow/testing/browserless).

### Playwright

- Read the [documentation](https://playwright.dev).
