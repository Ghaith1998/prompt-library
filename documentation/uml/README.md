# UML documentation

These Mermaid UML diagrams use the code's package-by-feature structure. Relationships are code-level dependencies (injection, mapping, repository calls, CDI discovery), not invented business integrations. External JPA/CDI/Quarkus interfaces are noted where needed but are not part of the project source inventory.

## Diagram guide

- **Solid line with open triangle (in diagrams):** generalization (`BaseEntity`, `BaseService`, and `Seeder`).
- **Solid link with cardinality:** JPA association or service/repository dependency.
- **Dashed link:** an important helper/adapter dependency that does not imply persistence ownership.
- `E extends BaseEntity` in `BaseService` is the only constrained service type variable; `PanacheRepository<E,Long>` is the injected type.
- Records are compact **immutable DTOs**, not persistent entities. Entity fields in the UML are private Java fields with JPA mapping; DTO fields are record components.
- Some implementation-only accessors/list operations are omitted from blocks for readability. Lookups and domain methods are included when they clarify behavior.

## Package-to-diagram map

| Diagram file | Coverage |
|---|---|
| [entities.md](entities.md) | `BaseEntity`, `Category`, `Folder`, `Prompt`, `PromptStatus`, `Tag`, all Panache repository classes, and association mappings |
| [services.md](services.md) | `BaseService`, feature service classes, constructor-injected collaborators, ownership helpers, and resource-to-service mapping |
| [integration.md](integration.md) | Security/auth resources, DTO records, Keycloak configuration and client, error mapping/exceptions, and all seed/discovery classes |

## Every current source class by package

### `prompt`

- Entity: `Prompt`
- Enum: `PromptStatus`
- Repository: `PromptRepository`
- Service: `PromptService`
- Resource: `PromptResource`
- DTO: `PromptRequest`, `PromptResponse`, `PromptMoveRequest`

### `folder`

- Entity: `Folder`
- Repository: `FolderRepository`
- Service: `FolderService`
- Resource: `FolderResource`
- DTO: `FolderRequest`, `FolderResponse`, `FolderUpdateRequest`, `FolderMoveRequest`

### `tag`

- Entity: `Tag`
- Repository: `TagRepository`
- Service: `TagService`
- Resource: `TagResource`
- DTO: `TagRequest`, `TagResponse`

### `category`

- Entity: `Category`
- Repository: `CategoryRepository`
- Service: `CategoryService`
- Resource: `CategoryResource`
- DTO: `CategoryRequest`, `CategoryResponse`

### `common`

- Shared model/service base: `BaseEntity`, `BaseService`
- Error response: `ApiError`
- Error mapper: `ExceptionMappers`
- Exceptions: `ConflictException`, `ResourceNotFoundException`, `InvalidCredentialsException`, `UpstreamException`

### `auth`

- Resource: `AuthResource`
- Config mapping: `KeycloakConfig`
- Outbound adapter: `KeycloakService`
- DTO: `LoginRequest`, `LoginResponse`, `RegisterRequest`, `RegisterResponse`

### `security`

- Identity adapter: `CurrentUser`
- Resource: `MeResource`
- DTO: `MeResponse`

### `seed`

- Discovery contract: `Seeder`
- Startup coordinator: `DatabaseSeeder`
- ID source: `SeedUsers`
- Implementations: `CategorySeeder`, `FolderSeeder`, `TagSeeder`, `PromptSeeder`

## Common reading mistakes avoided here

1. `Folder` and `Category` have different ownership semantics. Categories are global; folders are per-user, nested, and user-owned.
2. `prompt_tags` is a **join table**, not an entity, and has no owner column.
3. `BaseService` is not an entity finder with authorization. `PromptService.loadAccessible`, `FolderService.loadAccessible`, and `TagService.loadAccessible` own their respective access checks.
4. `CurrentUser` and `KeycloakService` are different responsibilities: token claims vs outbound IDP calls.
5. `SeedUsers` values represent Keycloak account IDs; they are not a `User` JPA entity and no users table exists.

## Customizing these UML diagrams

Mermaid links are intentionally direct. If the domain changes, edit the UML alongside the corresponding Java source:

- Schema additions → also alter `uml/entities.md`, DBML, and ER docs.
- New service collaborators → `uml/services.md` and the architecture flow notes.
- New request DTOs/resources → `uml/integration.md` and project structure docs.
- New seeders → `uml/integration.md` and startup/seeding documentation.
