# Project overview for managers and stakeholders

## One sentence

Prompt Library is a learning-oriented backend service for securely storing and organizing prompts, with owner-isolated workspaces, global admin-managed categories, hierarchical personal folders, and personal tags.

## Business goals

- Give each user a private place to create and manage prompt content.
- Preserve predictable ownership: normal users can operate on their own data, while administrators can inspect and manage across users.
- Show a maintainable REST architecture using idiomatic Quarkus, JPA/Panache, Flyway, Bean Validation, JWT/OIDC, and integration tests.
- Provide repeatable local development with companion containers and reproducible demo data rather than a dependency-free mocked database.

## Current capabilities

- Prompt CRUD, search in title/content, status (`DRAFT`, `PUBLISHED`, `ARCHIVED`), favorite flags, optional folder assignment, and explicit moves between owned folders.
- Personal folder hierarchy with nesting, rename, root/parent moves, cycle prevention, default folder creation, and protection against deleting non-empty/default folders.
- Owner-scoped tags with normalized names, search, per-owner unique constraint, CRUD, and deletion blocked while attached to prompts.
- Global category classification readable by authenticated users; category writes are admin-only.
- Headless login and self-registration using Keycloak, plus token introspection through `/api/me`.
- JWT-protected API with Swagger UI/OpenAPI, consistent error responses, SQL migrations, and throwaway integration-test environments.

## Personas

### Regular library user

Owns prompts, tags, and a personal folder tree. Creates prompts with tags, categories, favorites, and folder placement. Sees and modifies only their own records; foreign-owned IDs are indistinguishable from missing records (404).

### Administrator

Sees all records and can perform the same operations across users. Specifically manages global categories. Access is constrained only by normal data invariants: ownership must still match between a prompt and its folder, and folder trees cannot span owners.

### Developer/operator

Needs a local REST service for testing UI integrations or exercising real database/security behavior. Receives OpenAPI documentation and deterministic sample identities/content.

## Out of current scope

The following are deliberate limits of the existing service, not unfinished accidental omissions:

- A native frontend/mobile app, public/shared prompt community, and an administrative user-management UI.
- Pagination/cursor navigation, bulk operations, rich prompt editing, import/export, and prompt version history.
- Production Hardening for a distributed deployment (custom secrets/config management, externalized secrets rotation, backup automation, TLS setup, and rate limiting).
- Keycloak user records mirrored into the application database (there is no user table; ownership uses token subjects).

These can be added as requirements rather than inferred from current implementation.

## What stakeholders can verify locally

1. Docker Compose starts MySQL and imports Keycloak with 50 demo users.
2. Startup applies all V1–V6 DB migrations and creates roughly 100 records per main table (categories, folders, tags, prompts).
3. Every API except public login/register requires a JWT; Alice or Carol can demonstrate admin operations.
4. Bob/userNN logins illustrate owner isolation; other users' identities do not appear in their data listings.
5. Folder trees can be nested and moved without allowing cycles; prompt/folder ownership stays consistent.
6. The integration suite exercises the four feature surfaces (prompts, categories, folders, tags) and auth.

For exact endpoint and acceptance coverage see [requirements-and-acceptance.md](requirements-and-acceptance.md); deployment concerns are in [operations-and-release.md](operations-and-release.md).
