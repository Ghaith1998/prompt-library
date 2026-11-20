# Documentation map

This folder contains project-specific documentation generated from the actual current code, migrations, configuration, resource paths, services and tests. Start with the section for the question you are answering, not with test code.

| Section / file | Purpose |
|---|---|
| [database/README.md](database/README.md) | Detailed explanation of every table, column, index, relation, seed count and migration pattern |
| [database/schema.dbml](database/schema.dbml) | DBML source for complete current schema and FK relationships |
| [database/er-diagram.md](database/er-diagram.md) | Presentation-sized Mermaid entity-relationship overview, with multiplicity notes |
| [architecture/overview.md](architecture/overview.md) | Layer responsibilities, startup/HTTP/exception flow and architectural boundaries |
| [architecture/layers-and-flow.md](architecture/layers-and-flow.md) | Resource → service → repository execution details and feature-specific flows |
| [architecture/security-and-keycloak.md](architecture/security-and-keycloak.md) | OIDC validation, registration/login flows, identity adapter and realm notes |
| [architecture/persistence-and-seeding.md](architecture/persistence-and-seeding.md) | Migration versions, fetch/transaction rules, seeder beans/order, profiles and resets |
| [folder-structure/README.md](folder-structure/README.md) | Project tree, file responsibilities, test layout and where to change behavior |
| [uml/README.md](uml/README.md) | UML guide, package map and class diagram coverage for the complete application |
| [uml/entities.md](uml/entities.md) | JPA entities, value types, IDs, relations and type-safe Panache repositories |
| [uml/services.md](uml/services.md) | Feature service dependencies and resource-service coupling |
| [uml/integration.md](uml/integration.md) | Auth/security resources, DTOs, Keycloak integration, exceptions and seeder types |
| [project-description/overview.md](project-description/overview.md) | Executive summary, goals, goals-meter, personas and scope boundaries |
| [project-description/requirements-and-acceptance.md](project-description/requirements-and-acceptance.md) | Functional behavior, endpoint-level acceptance, NFR and walkthrough |
| [project-description/operations-and-release.md](project-description/operations-and-release.md) | Deployment checklist, configuration invariants, dependencies, operations and evidence |

## Reading routes

- **Get the app running:** use the root `README.md`, then refer to `architecture/persistence-and-seeding.md` for reset and demo data guidance.
- **Change prompt/folder behavior:** read `architecture/layers-and-flow.md`, the relevant feature service, then `uml/services.md` for its collaborators.
- **Understand the database:** start with `database/er-diagram.md` for the big picture and `database/README.md` for exact columns/indexes; use `schema.dbml` to render an external diagram if needed.
- **Review security:** `architecture/security-and-keycloak.md`, `CurrentUser`, and `KeycloakService`.
- **Brief stakeholders:** `project-description/overview.md`, `requirements-and-acceptance.md`, and `operations-and-release.md`.

The diagram text format is Mermaid for easy embedding in GitHub/GitLab documentation, IDEs, or many markdown renderers; normal reading does not require a diagram plugin.
