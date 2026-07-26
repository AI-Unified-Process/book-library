# Architecture: Package Structure

## Decision

The Book Library follows a **package-by-feature** layout. Each feature is a top-level
package under the application root, and inside every feature package there are exactly
two subpackages:

- `ui` — Vaadin views, dialogs, forms, and any UI-only helpers
- `domain` — jOOQ repositories, services, records, and any business logic

Cross-cutting concerns that are not part of a single feature (navigation shell,
configuration, security, shared UI components) live in a dedicated `core` package with
the same `ui` / `domain` split where applicable.

## Layering inside a feature

The flow of calls inside a feature is:

```
ui → service → repository
```

with one important rule: **the service layer only exists if it carries real business
logic.** If the use case is pure data access — read rows, render them; insert a row,
go back — the view talks to the repository directly, and there is no service. Adding
an empty service that only delegates `findAll()` to the repository adds a file, a Spring
bean, and a layer of indirection without buying anything; do not write it.

Add a service the moment the feature needs any of the following:

- Coordinating writes across more than one repository inside a transaction
- Validation or invariants that span multiple entities (e.g. "a member may not have more
  than five open loans")
- Side effects beyond the database (sending mail, publishing events, calling another
  system)
- Logic that needs to be reused from more than one view

For SimpleLibrary that means:

- **Catalog search** is a read-only query → `CatalogView` calls `BookRepository`
  directly, no `BookService`.
- **Borrow / return** mutates loan state, checks "is a copy available", and updates
  more than one row → `BorrowDialog` calls `LoanService`, which orchestrates
  `LoanRepository` and `BookRepository`.

## Why

- **Locality.** Everything you need to understand or change a feature lives in one
  place. You do not jump between `controllers/`, `services/`, `repositories/`, and
  `views/` to follow a single flow.
- **Replaceability.** A feature can be deleted, rewritten, or extracted into a module
  with no ripple effects, because nothing outside its package depends on its internals.
- **Clear seam between UI and data.** The `ui` / `domain` split keeps Vaadin out of the
  data layer and SQL out of the views. It is the only horizontal split we make inside a
  feature — anything finer is premature.
- **Pairs naturally with AIUP.** Each use case spec maps to one feature package. The
  `/implement` skill drops the view into `<feature>/ui` and the repository/service into
  `<feature>/domain` with no further routing decisions.

## Layout

```
ai.unified.process.demo.book.library
├── core                       # cross-cutting concerns
│   ├── configuration
│   ├── security               # SecurityConfig, AppUserDetailsService, CurrentUser, …
│   └── ui
│       ├── layout             # MainLayout, navigation
│       └── components         # shared Vaadin components
├── catalog                    # feature: browse / search books
│   ├── ui                     # CatalogView, BookGrid, …
│   └── domain                 # BookRepository, BookService, Book record
├── loan                       # feature: borrow / return books
│   ├── ui                     # BorrowDialog, ReturnDialog, ActiveLoansView, …
│   └── domain                 # LoanRepository, LoanService, Loan record
└── member                     # feature: members
    ├── ui
    └── domain
```

Feature names are nouns from the entity model and use case diagram, not verbs from the
use cases. *Borrowing* and *returning* both live in `loan` because they operate on the
same aggregate.

## Rules

1. **No feature-to-feature imports from `ui` to another feature's `domain`.** If a view
   in `catalog/ui` needs loan data, it goes through a service exposed by `loan/domain`,
   not directly into a `loan/ui` class.
2. **`ui` may depend on `domain` within the same feature; `domain` must not depend on
   `ui`.** This keeps the data layer headless and testable without Vaadin.
3. **Skip the service layer for pure data access.** Views may call repositories
   directly when there is no business logic to host. Introduce a service only once the
   feature meets one of the criteria listed above.
4. **Once a service exists, the view must go through it.** Mixed access (some calls
   through the service, others bypassing it into the repository) defeats the point of
   having the service and is the path most likely to violate invariants.
5. **No `controller`, `service`, `repository` top-level packages.** The split is by
   feature first; the role of a class is expressed by its name and its position inside
   `ui` or `domain`.
6. **`core` is for things shared by two or more features.** A class used by only one
   feature belongs in that feature, not in `core`.
7. **Records and DTOs live with the code that owns them.** A `Book` record returned by
   the catalog repository lives in `catalog/domain`. If another feature needs it, it
   imports from `catalog/domain`.

## Security

Authentication and authorization are part of the architecture, not a separate feature.

### Model

- **Identity is separate from the domain.** The `app_user` table holds authentication
  data only: `id`, `username`, `password_hash`, `role` (`MEMBER` or `LIBRARIAN`),
  `created_at`. No name, no email, no profile data — those belong to a domain entity.
- **`member` is a domain entity, not the identity.** Once the `member` feature is
  introduced (during the tutorial), the `member` table carries the patron's profile
  (name, email, …) and a `user_id` foreign key into `app_user`. A librarian is an
  `app_user` with role `LIBRARIAN` and **no** `member` row — librarians do not borrow
  books, so they do not need a patron profile.
- **Loans reference `member.id`, not `app_user.id`.** A loan belongs to a library
  patron, which is a `member`. The path from "currently logged-in user" to "their
  loans" goes through `member.user_id`.
- **No self service sign-up.** A librarian creates member accounts (a domain action
  that inserts an `app_user` row and a `member` row in the same transaction). The
  first librarian's `app_user` row is seeded on startup so the app is usable on first
  boot.

### Stack

Spring Security with a jOOQ-backed `UserDetailsService` (`AppUserDetailsService`) that
loads an `app_user` row by `username`, returns the password hash, and maps `role` to a
Spring authority (`ROLE_MEMBER` or `ROLE_LIBRARIAN`). The principal is
`AppUserDetails`, which exposes `appUserId` so callers can scope queries by the
logged-in user without re-querying. Vaadin's `VaadinSecurityConfigurer` wires the
login view and the post-login redirect; `AuthenticationContext` gives views access to
the current user.

### View access rules

Authorization lives on the view, expressed with JSR-250 annotations. The default is
deny — every routable view must declare who can reach it.

| Role         | Views                                                   |
|--------------|---------------------------------------------------------|
| `MEMBER`     | Search catalog, Borrow book, Return book, My loans      |
| `LIBRARIAN`  | Everything a member sees, plus Active loans, Manage catalog, Manage members |

In code:

- `@RolesAllowed({"MEMBER", "LIBRARIAN"})` on member-facing views.
- `@RolesAllowed("LIBRARIAN")` on librarian-only views.
- `@AnonymousAllowed` only on the login view.
- `@PermitAll` is **not** used — it hides the role intent and grants access to any
  authenticated user regardless of role, which is rarely what we want.

Vaadin uses the same annotations to hide menu items the current user cannot reach, so
the navigation in `MainLayout` does not need its own access checks.

### Package placement

- `core/security` holds the cross-cutting pieces: `SecurityConfig`,
  `AppUserDetailsService`, `AppUserDetails`, `Role`, `LoginView`, `SecuritySeed`, and
  the `CurrentUser` helper. `CurrentUser` reads Spring's `SecurityContextHolder`
  directly (no Vaadin), so it can be injected from `domain` code as well as views.
- Role-aware logic inside a feature (e.g. "a member sees only their own loans, a
  librarian sees everyone's") lives in that feature's `domain` layer and reads the
  current `app_user.id` via `CurrentUser`, then joins through `member.user_id` when it
  needs the patron. Views do not branch on roles; they call a service method that
  already scopes the query.

### Why this shape

- **Identity vs. domain are different concerns.** Mixing `username`/`password_hash`
  into `member` couples a domain entity to authentication mechanics. Splitting them
  means we can change auth (OAuth, SSO, …) without touching the patron model, and we
  can have non-patron users (the librarian) without inventing a fake `member` row.
- **Declarative access on views.** `@RolesAllowed` keeps the rule next to the view it
  protects and lets Vaadin handle menu visibility for free.
- **Scoping in the domain.** Putting the "only my loans" rule in the service means the
  same rule applies to every caller (view, future API, tests), instead of being
  re-implemented per view.

## Enforcement

ArchUnit tests enforce the rules above. They live in `src/test/java/.../architecture/`
and run as part of `./mvnw test`, so a violation breaks the build instead of relying on
review discipline.
