# Prompt Library (Quarkus learning project)

A small REST API to store prompts. Built to practice: **Flyway**, **ORM seeders**, **JPA/Panache entities and
relations**, **SmallRye OpenAPI**, Spring-style layering in Quarkus, **JUnit tests** and **Keycloak**.

## Prerequisites
- JDK 21, Docker (+ Docker Compose)
- Nothing else: the Maven wrapper is included (`./mvnw`)

## Quick start (dev = docker compose)

```bash
docker compose up -d          # MySQL :3306 and Keycloak :8180 (Keycloak needs ~30-60 s on first start)
./mvnw quarkus:dev            # app on :8080 with live reload
```

On startup the app runs the Flyway migrations, then the seeders (dev only).

| What | URL |
|---|---|
| Swagger UI | http://localhost:8080/q/swagger-ui |
| OpenAPI (yaml / json) | http://localhost:8080/q/openapi  ·  `?format=json` |
| Dev UI | http://localhost:8080/q/dev-ui |
| Keycloak admin console | http://localhost:8180  (admin / admin) |

Users imported into Keycloak: **alice / alice** (admin) and **bob / bob** (normal user).

## Try it

```bash
# log in (headless, password grant) and keep the token
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"alice"}' | jq -r .accessToken)

curl -s localhost:8080/api/me         -H "Authorization: Bearer $TOKEN"
curl -s localhost:8080/api/prompts    -H "Authorization: Bearer $TOKEN"
curl -s "localhost:8080/api/prompts?q=review" -H "Authorization: Bearer $TOKEN"

curl -s -X POST localhost:8080/api/prompts -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"My prompt","content":"Do X","categoryId":1,"tags":["Java","demo"]}'

# self-register a new user (goes through the Keycloak Admin API), then log in as that user
curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"carol","email":"carol@example.com","password":"S3cret-pass!","firstName":"Carol","lastName":"Coder"}'
```

In Swagger UI click **Authorize** and paste the `accessToken` (security scheme `jwt`).

Rules implemented: normal users only see/change **their own** prompts (others return 404), `admin` sees all and is the
only one who may create categories.

## Project layout (package by feature)

```
dev.learn.promptlibrary
├── prompt/     Prompt (entity) · PromptRepository · PromptService · PromptResource · dto/
├── category/   same pattern (1:n with Prompt)
├── tag/        same pattern (n:m with Prompt)
├── auth/       register + login against Keycloak (KeycloakService uses the JDK HttpClient)
├── security/   CurrentUser (reads the token) · /api/me
├── seed/       Seeder interface · DatabaseSeeder · CategorySeeder · PromptSeeder
└── common/     BaseEntity · exceptions · ExceptionMappers (= @ControllerAdvice)
```

Spring Boot → Quarkus cheat sheet: `@RestController` → `@Path` resource, `@Service` → `@ApplicationScoped`,
`@Autowired` → constructor injection / `@Inject`, `JpaRepository` → `PanacheRepository`, `@ControllerAdvice` →
`@ServerExceptionMapper`, `application.yml` profiles → `%dev.` / `%test.` / `%prod.` prefixes.

## Flyway cheat sheet

- Migrations: `src/main/resources/db/migration/V<n>__description.sql`. Applied in order, recorded in table `flyway_schema_history`.
- **Never edit a migration that has been applied anywhere.** Add a new `V5__...` instead (checksums are verified).
- V3 + V4 show the safe pattern for a NOT NULL column on a table that may already contain rows: add nullable → backfill → tighten.
- Hibernate never touches the schema (`schema-management.strategy=none`). Flyway is the only owner.
- Reset the dev database completely: `docker compose down -v && docker compose up -d` (Flyway + seeders rebuild everything).

### Connecting to an already existing database ("staging")
```bash
export DB_URL='jdbc:mysql://my-host:3306/my_db' DB_USER=... DB_PASSWORD=...
export OIDC_URL='https://my-keycloak/realms/prompt-library' KEYCLOAK_BASE_URL='https://my-keycloak'
export KEYCLOAK_CLIENT_SECRET=...
./mvnw quarkus:dev -Dquarkus.profile=staging
```
- Seeders stay **off** there (`app.seed.enabled=false` outside `%dev`).
- If that database already has tables that Flyway did not create, add
  `%staging.quarkus.flyway.baseline-on-migrate=true` and `%staging.quarkus.flyway.baseline-version=1`
  (treats V1 as already applied; only V2+ run). Use a copy of the database first.
- For a packaged run: `./mvnw package && java -Dquarkus.profile=staging -jar target/quarkus-app/quarkus-run.jar`.

## Seeders (Laravel style)
Implement `Seeder` and annotate the class `@ApplicationScoped` — it is picked up automatically, sorted by `order()`,
and uses entities + repositories (ORM), not SQL. They run only when `app.seed.enabled=true` (set in `%dev`).
Fake/dev data belongs in seeders; data that must exist in every environment belongs in Flyway migrations.

## Entities: where to look
`prompt/Prompt.java` is heavily commented: `@ManyToOne` (owning, LAZY), `@ManyToMany` + `@JoinTable` (owning side, `Set`),
`@Version` (optimistic locking), `@Enumerated(STRING)`, timestamps in `BaseEntity`, `equals/hashCode`.
`Category.prompts` and `Tag.prompts` are the inverse sides (`mappedBy`).
Gotcha: no open-session-in-view in Quarkus — services map entities to DTOs **inside** `@Transactional` methods.

## Tests

```bash
./mvnw test
```
Needs Docker: Quarkus Dev Services starts a throw-away MySQL and Keycloak for the test run (nothing configured under `%test`
on purpose). `@TestSecurity` fakes the logged-in user, so tests don't need real tokens.

## Keycloak notes
- Realm `prompt-library` is imported from `keycloak/import/prompt-library-realm.json` on first start.
  Without a volume, `docker compose down` resets Keycloak and the realm is re-imported.
- Client `prompt-library-api` (secret `dev-secret-change-me`) has *service accounts* (used to create users) and
  *direct access grants* (used for the headless login). The password grant is for learning/scripts; real frontends should
  use Authorization Code + PKCE.
- Only `admin` is checked as a role. Everybody authenticated is a normal user, so self-registered users work
  without extra role mapping.
- Keycloak 24+ requires first name, last name and e-mail, otherwise password logins fail with "Account is not fully set up".

## Troubleshooting
- **Maven cannot resolve `quarkus-bom:3.40.0`** → change `quarkus.platform.version` in `pom.xml` (see https://quarkus.io/releases).
- **`docker compose up` cannot pull the Keycloak image** → pick an existing 26.x tag in `docker-compose.yml`.
- **App starts but 401 on every call / OIDC errors on boot** → Keycloak is still starting; wait and retry.
- **`/api/auth/register` returns 502** → check the app log. Most likely the service account lacks permissions:
  Keycloak admin → Clients → `prompt-library-api` → *Service accounts roles* → assign `realm-management` → `manage-users`.
- **Seeded prompts not visible for alice/bob** → Keycloak must have been started from the bundled realm file
  (the seeders use the fixed user ids from it).
