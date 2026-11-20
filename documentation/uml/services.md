# UML: application services and REST boundaries

## Shared service base and feature implementations

```mermaid
classDiagram
    class BaseService~E~ {
        <<abstract>> E extends BaseEntity
        #repository(): PanacheRepositoryBase<E, Long>
        #entityName(): String
        +loadRequired(id: Long) E
        +validateNameUnique(name: String) void
    }

    class PromptService {
        -prompts: PromptRepository
        -categories: CategoryRepository
        -tags: TagRepository
        -folders: FolderService
        -currentUser: CurrentUser
        +list(q: String) List<PromptResponse>
        +get(id: Long) PromptResponse
        +create(request: PromptRequest) PromptResponse
        +update(id: Long, request: PromptRequest) PromptResponse
        +delete(id: Long) void
        +moveToFolder(id: Long, folderId: Long) PromptResponse
        +repository(): PromptRepository
        +entityName(): String
    }

    class FolderService {
        -folders: FolderRepository
        -prompts: PromptRepository
        -currentUser: CurrentUser
        +list() List<FolderResponse>
        +get(id: Long) FolderResponse
        +create(req: FolderRequest) FolderResponse
        +rename(id: Long, req: FolderUpdateRequest) FolderResponse
        +delete(id: Long) void
        +move(id: Long, req: FolderMoveRequest) FolderResponse
        +loadOwnedByOwner(id: Long, ownerId: String) Folder
        +ensureDefaultFolder(ownerId: String) Folder
        +repository(): FolderRepository
        +entityName(): String
    }

    class TagService {
        -tags: TagRepository
        -prompts: PromptRepository
        -currentUser: CurrentUser
        +list(q: String) List<TagResponse>
        +get(id: Long) TagResponse
        +create(req: TagRequest) TagResponse
        +update(id: Long, req: TagRequest) TagResponse
        +delete(id: Long) void
        +repository(): TagRepository
        +entityName(): String
    }

    class CategoryService {
        -categories: CategoryRepository
        -prompts: PromptRepository
        +list() List<CategoryResponse>
        +create(req: CategoryRequest) CategoryResponse
        +update(id: Long, req: CategoryRequest) CategoryResponse
        +delete(id: Long) void
        +repository(): CategoryRepository
        +entityName(): String
    }

    BaseService <|-- PromptService
    BaseService <|-- FolderService
    BaseService <|-- TagService
    BaseService <|-- CategoryService
    PromptService --> PromptRepository
    PromptService --> CategoryRepository
    PromptService --> TagRepository
    PromptService --> FolderService
    PromptService --> CurrentUser
    FolderService --> FolderRepository
    FolderService --> PromptRepository
    FolderService --> CurrentUser
    TagService --> TagRepository
    TagService --> PromptRepository
    TagService --> CurrentUser
    CategoryService --> CategoryRepository
    CategoryService --> PromptRepository
```

### Dependency semantics

- `PromptService` is the coordinator. Category and folder pointers are resolved through their repositories/helpers; `FolderService` is a coordinator of trees and shared default-folder behavior. Record conversion happens inside `@Transactional`.
- `FolderService` uses `PromptRepository` only for count-based in-use checks (not for accessing HTTP resources). Folder trees are scoped by `ownerId`; the helper `loadOwnedByOwner` is used by prompt placement so an admin cannot accidentally attach a prompt to another owner's tree.
- `TagService` mirrors usage checks through `PromptRepository`; global admin bypass does not make tag names globally unique (owner ID remains unique per owner).
- `CategoryService` uses the globally unique name validator from `BaseService` and a count by category before deletion. Its logical differentiation from owner-scoped services should remain in the service methods, not in `BaseService`.

## Resource-to-service contracts

```mermaid
classDiagram
    class PromptResource {
        +list(q) List<PromptResponse>
        +get(id) PromptResponse
        +create(req) RestResponse<PromptResponse>
        +update(id, req) PromptResponse
        +move(id, req) PromptResponse
        +delete(id) RestResponse<Void>
    }
    class FolderResource {
        +list() List<FolderResponse>
        +get(id) FolderResponse
        +create(req) RestResponse<FolderResponse>
        +rename(id, req) FolderResponse
        +delete(id) RestResponse<Void>
        +move(id, req) FolderResponse
    }
    class TagResource {
        +list(q) List<TagResponse>
        +get(id) TagResponse
        +create(req) RestResponse<TagResponse>
        +update(id, req) TagResponse
        +delete(id) RestResponse<Void>
    }
    class CategoryResource {
        +list() List<CategoryResponse>
        +create(req) RestResponse<CategoryResponse>
        +update(id, req) CategoryResponse
        +delete(id) RestResponse<Void>
    }

    class PromptService
    class FolderService
    class TagService
    class CategoryService
    class CurrentUser {
        +id(): String
        +username(): String
        +email(): Optional<String>
        +roles(): Set<String>
        +isAdmin(): boolean
    }
    class MeResource {
        +me() MeResponse
    }

    PromptResource --> PromptService
    FolderResource --> FolderService
    TagResource --> TagService
    CategoryResource --> CategoryService
    PromptService --> CurrentUser
    FolderService --> CurrentUser
    TagService --> CurrentUser
    MeResource --> CurrentUser
```

### REST rule map

- All resources shown are `@Authenticated`; category mutation methods also have `@RolesAllowed("admin")`.
- Creation methods construct HTTP 201 and `Location` in the resource; other responses are direct records or status-only results.
- No resource calls a repository or reads JPA fields directly. `CurrentUser` is constructed/inherited only where needed for scope.
- `MeResource` is the thin identity endpoint and delegates to `CurrentUser`. Login/register is a separate resource shown in [integration.md](integration.md).
