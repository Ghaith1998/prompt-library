# Layer responsibilities and request flows

## Project layering rules

### Resource (`*Resource`)

HTTP-only classes: URL mapping, security annotations, validation, DTO conversion where return status or `Location` headers matter, and OpenAPI documentation. They do not issue Panache queries and do not own transactions.

Example: `PromptResource.create` calls `service.create` then converts the returned DTO into an HTTP 201 response with `/api/prompts/{id}` in `Location`.

### Service (`*Service`)

Business operations, invariants, and transaction boundaries. Services:

- scope queries by owner (admins see everything),
- load domain objects,
- convert lazy JPA relations into response records inside transactions,
- create defaults (e.g. the user’s root default folder),
- perform delete safeguards (409 if the object is in use),
- translate lookup failures into `ResourceNotFoundException` and rule failures into `ConflictException`.

Any nested or repeated service logic stays in the concrete feature service. `BaseService` only shares ID loading (`loadRequired`), repository/entity-name resolution, and name-uniqueness validation used by globally named categories. It does not own authentication scope, prompt ownership, generic CRUD, DTO mapping, or subclass transaction methods.

### Repository (`*Repository`)

Panache data access only. Query details, named parameters, fetch graphs, sorting, and inexpensive counts stay here. They never depend on HTTP or caller context.

- `PromptRepository`: search with ownership and q parameters, fetch category/folder/tags, detail lookup with `list().stream().findFirst()`, and usage counters for delete checks.
- `FolderRepository`: find a default root, find by owner/name, list by owner with optional admin (null owner), and child counts.
- `TagRepository`: lookup or create per owner with name normalization; dynamic owner/q search.

## Prompt lifecycle details

### Create (`POST /api/prompts`)

1. The HTTP method validates `PromptRequest`: title ≤ 200 chars, content ≤ 20,000 chars, positive category ID, optional positive folder ID, optional status, optional favorite, and at most 20 tags.
2. `PromptService.create` sets owner to the caller's subject.
3. `apply` fetches the required category, resolves folder placement, and assigns scalar fields. If status/favorite is absent the entity default applies (`DRAFT`, false).
4. `resolveFolder` behavior:
   - Explicit folder ID → must belong to the prompt owner.
   - No folder ID on an existing prompt → retain the current folder.
   - New prompt with no folder → `ensureDefaultFolder(owner)` supplies `default` (root), lazily creating it when necessary.
5. `syncTags` null-filters, trims and lowercases tag names, discards empty strings, builds a set, removes unwanted associations, and adds required ones using `findOrCreate(prompt.ownerId, name)`.
6. The prompt is persisted; the response is mapped to `PromptResponse`, containing category/folder names and alphabetically sorted tag names.

### Get/update/delete/move

- Read and update use `findDetailed`, which loads category, folder, and tags explicitly, then checks accessibility. Foreign ownership becomes 404; admin access is allowed.
- Full updates apply the same DTO-to-entity rules as creation while retaining the owner and folder when folderId is omitted.
- `moveToFolder` verifies the caller can modify the prompt first, then validates that the target folder belongs to that same prompt owner. This also forbids admins from relocating content into another user's tree.
- Delete checks access and deletes the row. Tag join rows use database cascade, so prompt cleanup does not leave orphan join rows.

## Folder operations

### Default folder behavior

The root `default` is identified by `owner_id + name='default' + parent_id IS NULL`. `ensureDefaultFolder` is get-or-create, so every generic feature request can rely on it without a separate user-profile database. It is called on folder listing/create and by prompt fallback. Users are still stored in Keycloak; no realm-admin API call is made just to materialize a folder.

### Nested folder creation

`POST /api/folders` accepts an optional `parentId`:

- omitted/null: caller is owner;
- present: parent must be accessible to that caller, and the new folder inherits the parent's owner.

The service first ensures the default folder, checks the reserved name, and checks the per-owner name unique constraint. Cross-owner linking is rejected.

### Move a folder

`PUT /api/folders/{id}/move` accepts `parentId` (nullable). Normal users require ownership; admins can operate. When parentId is non-null, the parent must also have the same `owner_id` as the moving folder. A cursor walks from the requested parent up to the root:

- encountering the moving folder means it is a self/descendant move -> `409`;
- revisiting an already seen ID detects and rejects a malformed pre-existing cycle rather than looping forever.

The default folder is blocked from moving. A move to the current parent is a no-op.

### Delete folder

Delete is denied when default, when prompts reference the folder, or when child folders exist. The child/prompt checks are DB counts rather than loading collections. Empty non-default folders are deleted normally.

## Tag operations

Tags share the owner ID of the prompt creator. `PromptService.syncTags` uses the prompt owner's ID (important when an admin edits another person's prompt). Direct tag CRUD uses the caller's ID for create and loads by ID + owner/admin for reads and mutations.

- Missing/other-owned tag → 404 (admins bypass).
- Per-owner duplicate name → 409, with self excluded on rename.
- Attached to any prompt → delete rejected with 409.
- Admin gets a global q-scoped list; a normal user only receives their own tags.

## Error mapping

The shared `ApiError` record is `code + message`. HTTP status selection happens in `ExceptionMappers`:

| Type | Status | Code |
|---|---:|---|
| `ResourceNotFoundException` | 404 | `NOT_FOUND` |
| `ConflictException` | 409 | `CONFLICT` |
| `InvalidCredentialsException` | 401 | `INVALID_CREDENTIALS` |
| `UpstreamException` (Keycloak transport/protocol/response errors) | 502 | `KEYCLOAK_ERROR` |

Regular bean-validation payload errors are already 400 responses in Quarkus.
