# Persistence, migrations, and seed data

## Database responsibilities

Flyway is the only writer of DDL. Hibernate maps existing SQL tables and never runs schema-management changes. `quarkus.flyway.migrate-at-start=true` applies migrations as part of startup, using `flyway_schema_history` to track applied versions and validate checksums.

| Migration | Purpose |
|---|---|
| V1 | `categories`, `tags`, `prompts`, `prompt_tags` |
| V2 | `is_favorite` |
| V3/V4 | `prompts.owner_id` with nullable → backfill → NOT NULL |
| V5 | `tags.owner_id`, per-owner unique names and owner-split for shared tags |
| V6 | `folders` with optional self-reference, `prompts.folder_id` NOT NULL after default backfill |

Safe rules:

- Never edit an applied migration; create the next SQL file instead.
- Add optional columns as nullable first, backfill, then enforce NOT NULL when existing rows may exist.
- Keep the DB user capable of DDL and `REFERENCES` when creating foreign keys (V1, V6, and later relations).
- Exclusive schema discipline means changing a column's type/name in `application.properties`, Hibernate entity mapping, Flyway SQL, and tests should be done as one coherent change.

## Entity/session rules

Entities use `@ManyToOne(fetch=LAZY)` for `Prompt.category` and `Prompt.folder`. `PromptRepository.FETCH` explicitly fetches both associations and left-joins tags. `PromptResponse.from` reads those fields inside `@Transactional` service methods; this avoids surprises from detached/lazy relations when there is no open-session-in-view.

`findDetailed` deliberately calls `list().stream().findFirst()` because limiting with `firstResult()` can behave badly when fetching a collection association. `findOrCreate` methods remain in repository scope but callers run them within a transaction.

Delete-critical checks use `count()` (`category.id`, `folder.id`, or a join to tag) to avoid hydrating entire collections. Category/tag relationships have no application cascade, but `prompt_tags` has DB-level cleanup cascades. Default folders have both API protections and special handling.

## Seeder architecture

Seeders implement `Seeder`, are discovered through CDI `Instance<Seeder>`, and must be idempotent. `DatabaseSeeder.onStart` observes application startup, checks `app.seed.enabled`, sorts by `order()`, and invokes each bean sequentially. They are not SQL migrations and should not create schema.

| Order | Class | Current idempotency/count strategy |
|---:|---|---|
| 10 | `CategorySeeder` | 100 named categories, skip name if found |
| 15 | `FolderSeeder` | Ensure `default` + one nested `folder-01` per 50 users |
| 20 | `TagSeeder` | `tag-01` and `tag-02` per 50 users |
| 30 | `PromptSeeder` | Deterministic `Seed prompt NNN` until 100 prompts total |

Seed users come from `SeedUsers`: Alice (1), Bob (2), Carol (3), generated names (4–50). Their IDs must match the Keycloak realm import, otherwise new rows have no login owner. The local realm file carries corresponding credentials; DB rows themselves contain no password.

## Configuration profiles

| Profile | Datasource/security defaults | Seeding |
|---|---|---|
| `%dev` | Compose MySQL + imported local Keycloak, Dev Services off | on |
| `%test` | Nothing explicit; Dev Services MySQL + Keycloak | off by default |
| `%staging` | External DB/Keycloak supplied by environment variables | **currently on** |
| `%prod` | External DB/Keycloak, typically packaged jar | off |

The profile is selected with `-Dquarkus.profile=staging` or `prod`; development/testing usually must not leave `%test` pointed at an external DB. Inspection should show whether seeding is intended for that environment before starting, because current staging explicitly enables it.

## External services and reset

`docker compose up -d` supplies MySQL and Keycloak in dev. MySQL persists data in `mysql_data`; there is intentionally no Keycloak data volume. To rebuild cleanly:

```bash
docker compose down -v
docker compose up -d
./mvnw quarkus:dev
```

That removes **both** MySQL demo data and any old Keycloak realm state. If reseeding elsewhere, do not assume counts are exact when old records are retained: seed logic is additive/idempotent rather than destructive.

## Production/operational cautions

- Use a dedicated, nonempty database for first migration, because Flyway will create/modify tables on startup.
- Apply schema migrations before opening production traffic and use a DB account with the necessary DDL privileges.
- Avoid blanket production seeding; production seeding should be explicitly evaluated because category/folder/tag/prompt data is demonstration data.
- Users are in Keycloak, not in the relational schema; changing IDP identities can change ownership without changing SQL rows.
- Do not use a migration to perform uncontrolled destructive deletes. When testing data migrations, use a disposable schema with legacy rows first, then verify counts and relationship ownership.
