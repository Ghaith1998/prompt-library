# Project folder structure

## Repository root

```text
prompt-library/
├── docker-compose.yml       # local MySQL :3306 + Keycloak :8180, MySQL volume
├── keycloak/
│   └── import/
│       └── prompt-library-realm.json
├── mvnw, mvnw.cmd           # Maven wrapper, required instead of system Maven
├── pom.xml                  # Java 21 + Quarkus 3.40.1.2 BOM/plugins
├── AGENTS.md                # concise contributor/AI-agent operational guidance
├── README.md                # primary developer setup and feature tour
├── documentation/           # narrative, schema, architecture, UML, PM docs
├── prod.env, staging.env    # tracked deployment template values/placeholders
└── src/
    ├── main/
    │   ├── java/dev/learn/promptlibrary/
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/   # V1..V6 migration history
    └── test/java/dev/learn/promptlibrary/
```

There is no generated `target/` directory under version control, and no Node/frontend application in this repository.

## Package-by-feature main code

```text
dev.learn.promptlibrary
├── prompt/
│   ├── Prompt.java
│   ├── PromptStatus.java
│   ├── PromptRepository.java
│   ├── PromptService.java
│   ├── PromptResource.java
│   └── dto/
│       ├── PromptRequest.java
│       ├── PromptResponse.java
│       └── PromptMoveRequest.java
├── category/
│   ├── Category.java
│   ├── CategoryRepository.java
│   ├── CategoryService.java
│   ├── CategoryResource.java
│   └── dto/CategoryRequest.java, CategoryResponse.java
├── folder/
│   ├── Folder.java
│   ├── FolderRepository.java
│   ├── FolderService.java
│   ├── FolderResource.java
│   └── dto/
│       ├── FolderRequest.java
│       ├── FolderResponse.java
│       ├── FolderUpdateRequest.java
│       └── FolderMoveRequest.java
├── tag/
│   ├── Tag.java
│   ├── TagRepository.java
│   ├── TagService.java
│   ├── TagResource.java
│   ├── TagRequest.java
│   └── TagResponse.java
├── auth/
│   ├── AuthResource.java
│   ├── KeycloakConfig.java
│   ├── KeycloakService.java
│   └── dto/{Login,Register}{Request,Response}.java
├── security/
│   ├── CurrentUser.java
│   ├── MeResource.java
│   └── MeResponse.java
├── seed/
│   ├── Seeder.java
│   ├── DatabaseSeeder.java
│   ├── CategorySeeder.java
│   ├── FolderSeeder.java
│   ├── TagSeeder.java
│   ├── PromptSeeder.java
│   └── SeedUsers.java
└── common/
    ├── BaseEntity.java
    ├── BaseService.java
    ├── ExceptionMappers.java
    ├── ApiError.java
    └── {Conflict,ResourceNotFound,InvalidCredentials,Upstream}Exception.java
```

## Where to change each domain responsibility

| Change | Source locations |
|---|---|
| New prompt API field | `prompt/Prompt.java`, its migration, `PromptRequest/Response`, `PromptService` mapping, request tests |
| New folder/tree rule | `folder/FolderService.java`, entity repository for cheaper access/counts, `FolderResource` documentation, `FolderResourceTest` |
| Owner-security behavior | Feature service access helper plus `CurrentUser`, never only a resource annotation |
| New tag behavior | `tag/TagService.java`, `TagRepository`, `TagResource`, prompt tag synchronization if it affects automatic creation |
| Category admin behavior | `CategoryResource`, `CategoryService`, `CategoryResourceTest` |
| New database table | new `db/migration/Vn__...sql`, new entity and repository, updated documentation/database and UML |
| New claim / identity field | `CurrentUser` and `MeResponse`, tests around its fallback path |
| New Keycloak client credential | `application.properties` and `KeycloakConfig`, deployment env (`prod.env`/`staging.env`) |
| New startup seed data | dedicated `Seeder` with ascending `order()`, then run dev against a fresh DB and check row counts |
| Consistent error shape | `common/ExceptionMappers.java` plus a domain `RuntimeException` in `common/` |

## Test package layout

`src/test/java/dev/learn/promptlibrary/` is one package, named after HTTP surfaces rather than duplicating the entire source package tree:

- `AuthResourceTest.java` — public auth validation and `/api/me`.
- `PromptResourceTest.java` — prompt CRUD, default folders and moves, ownership and admin coverage.
- `CategoryResourceTest.java` — category admin/non-admin, duplicates, in-use checks.
- `TagResourceTest.java` — tag ownership, per-owner namespaces, q filtering and delete usage checks.
- `FolderResourceTest.java` — folder CRUD, nesting, root moves, default protection, cycle/usage constraints.

Every test uses `@QuarkusTest`; user identity comes from `@TestSecurity`, not a real token service. Dev Services supplies throwaway MySQL and Keycloak instances for test runs, so it must be able to reach Docker. `%test` deliberately leaves datasource/OIDC URLs empty.

## Documentation map

- `documentation/database/`: DBML source, full schema narrative, presentation ER diagram.
- `documentation/architecture/`: service boundaries, request flows, persistence/seeding, Keycloak/OIDC.
- `documentation/folder-structure/`: this project map and change entry points.
- `documentation/uml/`: class/domain/service/integration diagrams covering every current source class.
- `documentation/project-description/`: scope, objectives, acceptance, release/operations risks.

Use the root `README.md` for day-to-day setup; keep this directory focused on architecture and behavior context that is easy to miss when changing a feature.
