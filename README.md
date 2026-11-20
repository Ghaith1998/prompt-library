# Prompt Library (Quarkus learning project)

A backend-only REST API for private prompt libraries. It demonstrates **Quarkus 3.40**, **JPA/Hibernate ORM + Panache**, **Flyway**, **MySQL**, **Keycloak/OIDC**, SmallRye OpenAPI, Laravel-style idempotent seeders, and Rest Assured integration tests.

## What it includes

- **Prompts:** CRUD, text search, status (`DRAFT`, `PUBLISHED`, `ARCHIVED`), favorites, ownership, category classification, folder assignment, and explicit folder moves.
- **Folders:** user-owned unlimited nesting, optional parent on creation, rename, move to root/parent, and cycle protection.
- **Tags:** user-owned normalized lowercase names, per-owner uniqueness, search, CRUD, and in-use delete protection.
- **Categories:** global shared classes, readable by all authenticated users; only admins can write.
- **Keycloak:** bearer JWT resource server integration, a password-grant login helper, self-registration through the Admin API, and `/api/me`.
- **Persistence:** Flyway V1–V6, MySQL containers, ordered idempotent startup seeders, and Dev Services for integration tests.
- **Documentation:** DBML/ER diagrams, architecture notes, UML class diagrams, project-structure guide, and manager/stakeholder documents under [`documentation/`](documentation/README.md).

If you only need startup instructions, read the next three sections and then the key login example. For behavior/implementation details use the documentation map at the end.

## Prerequisites

| Requirement | Why | Check command |
|---|---|---|
| JDK 21 | Java compiler/runtime | `java -version` (should report 21.x) |
| Docker Desktop / Engine + Compose v2 | Local MySQL & Keycloak; Dev Services for tests | `docker version` and `docker compose version` |
| `curl` (included in modern Windows/macOS/Linux) | API smoke tests | `curl --version` |
| `jq` (optional, Linux/macOS) | Extract login response token | `jq --version` |

Install **no Maven**. Use the included wrapper (`./mvnw`, `mvnw.cmd`); it fetches Maven automatically.
No local MySQL install or Keycloak installation is needed in dev—the Compose file provides them.

Suggested distributions:

- JDK: [Eclipse Temurin](https://adoptium.net/) (macOS might use [SDKMAN!](https://sdkman.io/), Windows might use a JDK installer)
- Docker: [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows/macOS) or Docker Engine + Compose plugin (Linux)

## Quick start (local dev)

### 1. Verify Java, Docker, and the database ports

Use `:8080`, `:8180`, and `:3306` for the app, Keycloak, and MySQL. Stop other services on them if necessary.

```bash
java -version
docker version
docker compose version
```

### 2. Start local dependencies

In the repository root:

```bash
docker compose up -d
```

This starts MySQL on **3306** and Keycloak on **8180**, including the bundled **50-user** realm. Wait for both to be ready (usually ~30–60 seconds for the first Keycloak start).

Useful checks (same command on all three OSes):

```bash
docker compose ps
docker compose logs mysql
docker compose logs keycloak
# follow Keycloak logs; Ctrl+C to stop following, not to stop the container
docker compose logs -f keycloak
```

Windows tip: `docker compose` is used in PowerShell/CMD too, not `docker-compose` for the current plugin.

### 3. Start the API

Bash on Linux/macOS:

```bash
./mvnw quarkus:dev
```

Windows PowerShell or CMD:

```powershell
.\mvnw.cmd quarkus:dev
```

Open:

| Service | Address | Local credentials / note |
|---|---|---|
| REST API | http://localhost:8080 | all `/api` routes described below |
| Swagger UI | http://localhost:8080/q/swagger-ui | interactive API tests, add bearer token under Authorize |
| OpenAPI | http://localhost:8080/q/openapi or `?format=json` | generated specification |
| Dev UI | http://localhost:8080/q/dev-ui | local diagnostic dev interface |
| Keycloak Admin Console | http://localhost:8180 | `admin / admin` |
| MySQL | `localhost:3306` | DB `promptlib`, user/password `promptlib/promptlib` |

Startup performs migrations then seeding. First query may require waiting for Keycloak to finish booting.

### 4. Log in and call the API

The bundled realm includes these sample users (all with matching fixed ownership IDs):

| Username | Password | Realm roles |
|---|---|---|
| `alice` | `alice` | `admin`, `user` |
| `bob` | `bob` | `user` |
| `carol` | `carol` | `admin`, `user` |
| `user04` … `user50` | each user's own username | `user` |

Linux/macOS (bash, `jq` required just for convenience):

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"alice"}' | jq -r .accessToken)

curl -s http://localhost:8080/api/me -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/prompts -H "Authorization: Bearer $TOKEN"
```

Windows PowerShell (no `jq` required):

```powershell
$login = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/auth/login' `
    -ContentType 'application/json' `
    -Body @{ username = 'alice'; password = 'alice' } | ConvertTo-Json
$token = $login.accessToken
$headers = @{ Authorization = "Bearer $token" }

Invoke-RestMethod -Uri 'http://localhost:8080/api/me' -Headers $headers
Invoke-RestMethod -Uri 'http://localhost:8080/api/prompts' -Headers $headers
```

Swagger UI alternative: obtain `accessToken`, click **Authorize**, and paste it as provided (without any prefix).

Register a new Keycloak account through the application (choose another unique username if this one has already been used):

```bash
curl -s -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"newuser01","email":"newuser01@example.com","password":"S3cret-pass!","firstName":"New","lastName":"User"}'
```

### 5. Stop / restart / clean reset

`/tmp` is not used for persistence.

```bash
docker compose down          # stop Compose services; MySQL data remains in mysql_data
docker compose down -v       # also delete MySQL data volume (fresh migrations + seeds next run)
docker compose up -d
```

Keycloak has no data volume here, so Compose down/up starts a fresh imported realm. Any already imported realm is **not** updated by changing `keycloak/import/prompt-library-realm.json`; create it fresh via `docker compose down -v` when changing it.

## API overview

### Auth

| Route | Method | Auth | Behavior |
|---|---|---|---|
| `/api/auth/register` | POST | Public | Creates user in Keycloak via Admin API; 201 / 400 / 409 / 502 |
| `/api/auth/login` | POST | Public | Password-grant tokens for learning and scripts |
| `/api/me` | GET | Authenticated | Returns caller id, username, email if present, roles |

For an actual frontend, prefer Authorization Code + PKCE directly against Keycloak instead of sharing user passwords with this helper login route. Users live in Keycloak, **never in a local users table**.

### Prompts (`/api/prompts`)

| Method | Path | Behavior |
|---|---|---|
| GET | `/api/prompts` | List (admin all, others' own only), `?q=` title/content substring, newest first |
| GET | `/api/prompts/{id}` | Read a prompt by id (or foreign id as 404) |
| POST | `/api/prompts` | Create as owner. `categoryId` required; optional `folderId`, `status`, `favorite`, `tags`; absent folderId means the owner’s `default` folder |
| PUT | `/api/prompts/{id}` | Replace an existing prompt preserving owner; missing `folderId` retains the current folder |
| PUT | `/api/prompts/{id}/move` | Move to another folder using `{ "folderId": <id> }`, provided it belongs to the prompt owner |
| DELETE | `/api/prompts/{id}` | Delete prompt; cascades associated prompt_tag links |

Creation example (after creating or selecting an existing category with id 1 and folder with id 7):

```bash
curl -s -X POST localhost:8080/api/prompts -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Backend example","content":"You are a senior Java reviewer...","categoryId":1,"folderId":7,"tags":["java","review"]}'
```

Status name (`PromptStatus`), category/folder ids and names, ownerId, sorted tag names, favorite flag, createdAt and updatedAt appear in the returned DTO.

### Folders (`/api/folders`)

- GET `/api/folders`: own tree as flat `id/name/ownerId/parentId/isDefault` (admin sees all).
- GET `/api/folders/{id}`: fetch one owned/accessible folder.
- POST `/api/folders`: `{ "name": "Backend", "parentId": null }` (root), or pass a parent id for nesting.
- PUT `/api/folders/{id}`: rename; duplicate names per owner forbidden.
- DELETE `/api/folders/{id}`: empty non-default folders only (204).
- PUT `/api/folders/{id}/move`: `{ "parentId": <newParentId> }`; `null` puts folder at root.

Default/root folder is automatically created when necessary, and can be seen from folder lists. You cannot rename, move, or delete it. Moving under itself or one of its descendants is a 409 (cycle detection walks the actual parent chain). Deleting a folder with prompts or child folders returns 409 as well.

### Tags (`/api/tags`)

- GET `/api/tags` plus optional `?q=`: own tags only; admins see all with substring filtering.
- GET `/api/tags/{id}`: read a tag by id, or 404 if not yours.
- POST `/api/tags`: creates an owner-scoped tag, trims and lowercases names; duplicate name per owner returns 409.
- PUT `/api/tags/{id}`: rename a tag, with a per-owner duplicate check.
- DELETE `/api/tags/{id}`: delete an unused tag; any existing prompt association returns 409.

Two users can own identically named tags: DB uniqueness is `(owner_id, name)`, not a global name.

### Categories (`/api/categories`)

- GET `/api/categories`: globally shared sorted names (authenticated users).
- POST/PUT/DELETE + `/{id}`: **admin-only**.
- Duplicate names or deleting a category referenced by prompts → 409.
- Non-admin writes → 403.

### Access rules summary

- Normal users only list/read/change their own prompts, folders and tags. A foreign-owned ID returns **404**, not 403.
- `admin` can see and manage all prompts/folders/tags and is the only role allowed to create, rename or delete categories.
- Prompts must stay in a folder owned by the prompt owner; folder trees cannot mix owners. This invariant is enforced even for admins.
- The `default` folder is a per-user root, created lazily, and cannot be renamed, moved or deleted.
- Prompt/status/tag/category/folder relations are owned by the service layer; resources only handle HTTP, security annotations, validation and response construction.

## Project structure and Quarkus mapping

```text
dev.learn.promptlibrary
├── prompt/       Prompt entity · repository · service · resource · dto/
├── folder/       Folder tree entity · repository · service · resource · dto/
├── tag/          owner-scoped Tag entity · repository · service · resource · DTOs
├── category/     global Category entity · repository · service · resource · dto/
├── auth/         public login/register resource · KeycloakConfig · JDK HttpClient adapter · DTOs
├── security/     CurrentUser token adapter · /api/me
├── seed/         Seeder contract · startup coordinator · ordered category/folder/tag/prompt seeders
└── common/       BaseEntity · BaseService · domain exceptions · uniform exception mapper · ApiError
```

The feature flow is **Resource → Service → Panache Repository → DTO**. DTO mapping happens inside the service's transaction because Quarkus does not enable open-session-in-view. `BaseService` only shares ID lookup/name validation; ownership, transactions, folder rules and tag reconciliation stay in each concrete service.

Spring Boot → Quarkus cheat sheet:

| Spring Boot | Quarkus / Jakarta REST |
|---|---|
| `@RestController` / `@RequestMapping` | `@Path` resource class and method |
| `@Service` | `@ApplicationScoped` (plus `@Transactional`) |
| `@Autowired` fields | Single constructor injection / `@Inject` |
| `JpaRepository` | `PanacheRepository` |
| `@ControllerAdvice` / `@ExceptionHandler` | `@ServerExceptionMapper` |
| `application.yml` profiles | `%dev.`, `%test.`, `%staging.`, `%prod.` prefixes |
| Spring Security filter chain | Quarkus OIDC resource server + annotations such as `@Authenticated` / `@RolesAllowed` |

## Tests and build

Dev tests start **their own disposable MySQL and Keycloak**, not the Compose ones. Docker must be running. No live tokens are necessary because tests use `@TestSecurity`.

Bash:

```bash
./mvnw test
./mvnw test -Dtest=PromptResourceTest
./mvnw test -Dtest=FolderResourceTest
./mvnw test -Dtest=TagResourceTest
```

Windows PowerShell/CMD:

```powershell
.\mvnw.cmd test
.\mvnw.cmd test '-Dtest=PromptResourceTest'
.\mvnw.cmd test '-Dtest=FolderResourceTest'
.\mvnw.cmd test '-Dtest=TagResourceTest'
```

Compile without starting any containers:

```bash
./mvnw -DskipTests test-compile
```

Package and run a normal JVM JAR:

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

The packaged run defaults to `prod` and should use environment-provided DB/Flyway/Keycloak values. Do not point it at production data without checking migration + seeding settings first. `prod` disables seeders; `%dev` and `%staging` currently enable them.

## Deployment and configuration notes

Configuration lives in `src/main/resources/application.properties`.

| Profile | Purpose | Infra | Seeding |
|---|---|---|---|
| `%dev` (default in `quarkus:dev`) | Local Compose | localhost:3306 and :8180 with imported realm | on |
| `%test` | Isolated integration tests | Quarkus Dev Services | off by default |
| `%staging` | External staging environment | env-based DB/Keycloak | on |
| `%prod` | External deployed environment, often a JAR | env-based DB/Keycloak | off |

The important external variables are:

```text
DB_URL, DB_USER, DB_PASSWORD
OIDC_URL, OIDC_CLIENT_ID
KEYCLOAK_BASE_URL, KEYCLOAK_REALM
KEYCLOAK_LOGIN_CLIENT_ID, KEYCLOAK_LOGIN_CLIENT_SECRET
KEYCLOAK_REGISTER_CLIENT_ID, KEYCLOAK_REGISTER_CLIENT_SECRET
```

`prod.env` and `staging.env` are **tracked templates** for these values; `.env` is local-only and ignored by git. Never put production passwords in tracked env templates. OIDC validation and direct Keycloak calls (`quarkus.oidc.*` vs `app.keycloak.*`) must agree on host, realm, and client.

Current staging/prod defaults use the local-style `prompt-library` realm and `prompt-library-api` client, and no access-token audience check to match that realm. If using `promptlib-edge` / split clients again, enable audience validation and matching Keycloak audience mapper deliberately.

### Keycloak notes

- Realm `prompt-library` is imported from `keycloak/import/prompt-library-realm.json` on first startup. `--import-realm` does not update an existing realm; recreate the Keycloak data store if you changed that file.
- Client `prompt-library-api` (secret `dev-secret-change-me`) has **service accounts** for `/api/auth/register` and **direct access grants** for `/api/auth/login`.
- The password grant is suitable for learning and scripts. A real frontend should use Authorization Code + PKCE directly against Keycloak.
- Only the `admin` role is checked explicitly. All other authenticated users follow normal-user ownership rules. Keycloak 24+ requires first name, last name and e-mail or password login fails with “Account is not fully set up”.
- The realm contains 50 fixed user IDs matching `SeedUsers`; changing or recreating users with different IDs can make existing seeded prompts/folders/tags appear ownerless.

## Database migrations and seed data

Flyway exclusively owns SQL DDL (`quarkus.hibernate-orm.schema-management.strategy=none`). Current migrations cover:

1. Base categories/tags/prompts/prompt_tags
2. Favorite field
3. Prompt owner id, then mandatory ownership
4. User-owned tags with owner/name uniqueness
5. User-owned folders + required prompt folder assignment

Always add a new version—never edit a migration after some environment applied it. Add NOT NULL fields using nullable → backfill → tighten patterns. Foreign keys require DB-level `REFERENCES` privileges (discovered relevant for prod/staging).

Dev seed order is **Category → Folder → Tag → Prompt** (`order()` 10/15/20/30):

- 50 Keycloak user IDs built deterministically in `SeedUsers` and matched to the realm import.
- 100 categories, 100 folders (default + one nested folder per user), 100 owner-scoped tags, 100 prompts (2 per user).
- Two tags per generated prompt by default; the join count is larger than 100 because joins store one row per prompt-tag pair.
- Seeders are idempotent through name/title lookups; existing records are not removed. Repeated runs can yield larger totals if old or custom data remains.

### Flyway cheat sheet

- Migrations live in `src/main/resources/db/migration/V<n>__description.sql` and are recorded in `flyway_schema_history`.
- **Never edit a migration that has been applied anywhere.** Current head is V6; the next migration is `V7__...` (checksums are verified).
- Existing-table NOT NULL additions use the nullable → backfill → tighten pattern demonstrated by V3/V4. Owner splits and folder backfills are demonstrated by V5/V6.
- Hibernate never changes the schema (`schema-management.strategy=none`); Flyway is the only DDL owner.
- Reset the local database completely with `docker compose down -v && docker compose up -d` (this also recreates Keycloak).
- If adopting an existing database that was not created by Flyway, use a copy and configure:
  ```properties
  %staging.quarkus.flyway.baseline-on-migrate=true
  %staging.quarkus.flyway.baseline-version=1
  ```
  This treats V1 as already applied and is not appropriate for a new empty schema.
- For a packaged staging run:
  ```bash
  ./mvnw package
  java -Dquarkus.profile=staging -jar target/quarkus-app/quarkus-run.jar
  ```

### Seeder design

Seeders are reusable CDI beans: implement `Seeder`, annotate the class `@ApplicationScoped`, and return an ordering value. `DatabaseSeeder` discovers them through `Instance<Seeder>`, sorts by `order()`, and runs them only when `app.seed.enabled=true`. They use repositories/entities (ORM), never ad-hoc SQL, and must be idempotent because they may run on every startup.

Fake/demo data belongs in seeders; structural or required production data belongs in Flyway migrations. `%dev` and currently `%staging` enable seeders, while `%prod` disables them. Review that setting before starting any external environment.

### Entities: where to look

- `prompt/Prompt.java` demonstrates owning lazy `@ManyToOne` category/folder relations, owning `@ManyToMany` tags via `prompt_tags`, `@Version` optimistic locking, string enums and the task-specific tag helpers.
- `folder/Folder.java` demonstrates nullable self-reference (`parent`) plus inverse child/prompt sets without cascade.
- `category/Category.java` and `tag/Tag.java` are inverse/read-only mirrors of the prompt side. Tags add per-owner composite uniqueness; categories use a global unique name.
- `common/BaseEntity.java` centralizes generated IDs and creation/update timestamps.
- Lazy fields are not serialized from resources: services map entities to response records inside `@Transactional` methods.

## Troubleshooting

- **`java` / `mvnw` cannot find Java:** install Temurin 21 or set `JAVA_HOME` to JDK 21 before running the wrapper.
- **Offline wrapper download fails:** wrapper requires network on first use; download it via another machine / cache Maven artifacts before running.
- **`quarkus-bom` resolution problems:** verify `quarkus.platform.version` in `pom.xml` (currently 3.40.1.2); do not assume arbitrary versions exist.
- **Keycloak image cannot be pulled:** pick a currently published 26.x tag in `docker-compose.yml`/staging compose file.
- **Keycloak port/realm/admin page unavailable:** container is still starting, crashed, or using a different mapping/config. Inspect `docker compose logs keycloak`.
- **401/OIDC errors during startup:** Keycloak may still be booting; wait ~30–60 seconds and retry. Also verify issuer, realm and host match between OIDC validation and outbound Keycloak settings.
- **Registration returns 502:** Keycloak service account requires `realm-management.manage-users`.
- **Realm has no newly added users:** `--import-realm` does not update an existing realm. Remove its data store and start fresh, or add/update users through Admin API.
- **Seeded prompts are invisible:** Keycloak must have been started from the bundled realm file. Seed data uses fixed IDs (`6f1c2b0e-...`) matching `SeedUsers`; users created with different IDs cannot own those rows.
- **401 on valid-looking endpoints:** use `Authorization: Bearer <accessToken>`, ensure realm URLs match on both outbound Keycloak settings and incoming OIDC validation.
- **Startup hangs on `localhost:8080` usage:** app may still be starting, or another process already has the port; change `quarkus.http.port`.
- **Tests fail with "Docker daemon" error:** `mvn test` uses disposable containers. No `%test` database config is intended.

## Documentation map

- [`documentation/README.md`](documentation/README.md): documentation index and reading paths
- [`documentation/database/`](documentation/database/README.md): schema tables, DBML and simplified ER diagram
- [`documentation/architecture/`](documentation/architecture/overview.md): end-to-end layer responsibilities and flows
- [`documentation/folder-structure/`](documentation/folder-structure/README.md): repository map and where edits belong
- [`documentation/uml/`](documentation/uml/README.md): UML coverage for entity, service, resource, integration, DTO, error, seeder and repository types
- [`documentation/project-description/`](documentation/project-description/overview.md): stakeholder overview, acceptance coverage, operations and release notes
