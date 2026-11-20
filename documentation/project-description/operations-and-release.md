# Release and operations notes

## Deployment checklist

1. Confirm a compatible JDK 21 and Maven wrapper is available on the deployment machine.
2. Prepare a dedicated MySQL database with DDL rights, including `REFERENCES` for relational foreign keys.
3. Decide how Keycloak should be provisioned. A fresh H2 import needs a data directory; a MySQL-backed Keycloak must have an empty/recreated schema when a new realm import is intended.
4. Ensure the realm name/client in `application.properties` and environment entries matches the exact Keycloak import. Realm import files do not update an already existing realm.
5. Supply DB/OIDC/Keycloak settings through deployment environment values:
   - `DB_URL`, `DB_USER`, `DB_PASSWORD`
   - `OIDC_URL`, `OIDC_CLIENT_ID`
   - `KEYCLOAK_BASE_URL`, `KEYCLOAK_REALM`
   - `KEYCLOAK_LOGIN_CLIENT_ID`, `KEYCLOAK_LOGIN_CLIENT_SECRET`
   - `KEYCLOAK_REGISTER_CLIENT_ID`, `KEYCLOAK_REGISTER_CLIENT_SECRET`
6. Enable migration at startup and inspect Flyway output for every new migration before accepting traffic.
7. Decide `app.seed.enabled` explicitly: `%dev` and `%staging` are currently enabled; `%prod` is disabled.
8. Run `./mvnw package` and start `java -jar target/quarkus-app/quarkus-run.jar` if using packaged deployment. Default packaged profile is `prod` (can be overridden).
9. Smoke test a token grant and `/api/me`, followed by an owner-scoped endpoint (`/api/folders` or `/api/tags`).
10. Keep environment secrets out of tracked env templates; `.env` is local-only and ignored by git.

`prod.env` and `staging.env` are templates, not an authoritative secure secret store. Values in version control should remain examples/dev placeholders, never real credentials.

## Operational dependencies

| Dependency | Required for | Failure signal / check |
|---|---|---|
| MySQL | Persistent prompts/folders/tag/category records | App startup cannot connect; Flyway error; JDBC URL or privileges wrong/DB not available |
| Flyway | Validated schema migration | Failed migration in startup due existing data, SQL privilege or engine incompatibility |
| Keycloak | Token validation, login, registration | 401 when checking tokens, invalid_client, connectivity failures, or 502 from registration |
| Docker/Dev Services | Local infrastructure and test containers | Test setup fails when Docker daemon is off or resources cannot start |
| JDK 21 | Compile/run and Java 21 records/text-block/var inference source constructs | Unsupported feature compilation/runtime mismatch |

## Configuration invariant

OIDC validation and direct outbound Keycloak calls are two separate configuration paths. If realm/host/client changes, update `quarkus.oidc.auth-server-url` as well as `app.keycloak.*`. With local-style `prompt-library-api`, do not force access-token audience validation unless you deliberately add a matching audience mapper.

Seeding is profile-dependent. `%staging` has it enabled as a deliberate demonstration setting. Operators should change it to false if staging should not append demo data at startup.

## Database migration operations

- Migration history is sequential; latest is V6. Add the next SQL version rather than modifying deployed files.
- MySQL/Hibernate are deliberately split: Flyway pays the schema bill, Hibernate maps it with `schema-management.strategy=none`.
- Validate each ownership migration (V3–V6) with existing records in a disposable schema. Empty test schema does not exercise backfill paths.
- For FK changes ensure the application DB user has `REFERENCES`, `CREATE`, `ALTER`, `DROP`, `INDEX` and other appropriate permissions.
- Never assume a removed Keycloak data volume wiped a MySQL `keycloak` schema. External database state and container volume state are different things.

## Health and troubleshooting

| Symptom | Most likely check |
|---|---|
| Startup reports missing datasource URL in test | Install/start Docker; tests intentionally rely on Dev Services |
| App/JWT calls fail immediately after Compose startup | Keycloak readiness. Wait ~30–60s and retry |
| Registration fails with 502 | Check service-account client credentials and `realm-management.manage-users` role in Keycloak |
| Newly changed realm receives no users | Realm was already imported previously. Reset the IDP data store, or create/update users with Admin API rather than re-importing |
| Foreign claim checks reject token | Confirm issuer URL (`OIDC_URL`) and client realm match. Do not keep obsolete audience constraints from a different client setup |
| Dev/demo prompts invisible to expected user | Keycloak user ID must match the ownership UUID in the realm file (`6f1c2b0e-...NNNN...NNNN`) |
| Folder deletion seems ignored | Default folder or it still has prompts/children. These return 409 by design |
| Build cannot resolve Quarkus BOM | Adjust `quarkus.platform.version` only after confirming the specific BOM exists; current pom uses 3.40.1.2 |

## Release evidence

- Latest verified full integration run: **52 tests, 0 failures, 0 errors** across auth, categories, prompts, folders, and tags.
- Expected database indexes and constraints: category global unique names; per-owner tag/folder unique names; `fk_folders_parent`; `fk_prompts_folder`; `fk_pt_prompt` and `fk_pt_tag` with cascade rules.
- Documentation generated under `documentation/` for schema, architecture, code structure, UML, stakeholder overview, scope and release notes.
