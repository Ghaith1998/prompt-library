# Functional requirements and acceptance coverage

## Functional coverage

| Area | Behavior delivered | Acceptance check |
|---|---|---|
| Authentication | Login and registration are public; other routes require a token | `POST /api/auth/login` returns a bearer access token; protected endpoints return 401 with no token |
| Identity | `/api/me` exposes subject, username, email when present, and roles | Token for Alice yields `admin`/`user`, username alice, and fixed subject |
| Prompt ownership | A prompt belongs to the token subject that created it; another user's ID returns 404 | Bob cannot view/edit/delete Alice's prompts; Alice (admin) can access them |
| Prompt fields | Title/content, category, favorite, status, owner, folder, timestamps, sorted normalized tags | Create with categoryId, optional folderId, favorite, status, and tags |
| Prompt search | Bonus `q` on list filters lowercase substring in title and content; default ordering newest first | `GET /api/prompts?q=review` returns only matches; admin sees all matching prompts |
| Prompt folders | Default on create; optional explicit folderId; folder is preserved on update if omitted; move supported | No folderId lands in owner’s `default`; move validates owner before changing folder |
| Categories | List for users; create/rename/delete only for `admin`; duplicates and in-use deletes blocked | Non-admin write returns 403; duplicate returns 409; a referenced category cannot be deleted |
| Folder CRUD | Owner-only create/read/update/delete; admin bypasses access checks; flat response with parentId and isDefault | Regular user sees own folders, admin sees all. Unknown/foreign ID returns 404 |
| Folder hierarchy | Optional `parentId` at creation; nodes form one-owner tree | Creating child sets parentId and owner; invalid/cross-owner parents rejected |
| Folder moves | `PUT /api/folders/{id}/move`; null means root; reject self/descendant/cyclic moves | Moving a child under another node works; descendant/self requests return 409 |
| Default folder safety | Each user has one root default folder on first relevant operation | Default cannot be renamed, moved, or deleted (409) |
| Folder deletion | Empty non-default folders only; children or prompts cause 409 | Deleting an empty folder yields 204; a repeat request returns 404 |
| Tag ownership/CRUD | Tags have caller ownership and per-owner name uniqueness; admins can access all | Non-owner mutations return 404; same tag name is valid for different owners |
| Tag normalization/filter | Names trimmed/lowercase; optional q substring search; sorted by name | `POST /api/tags` with `"  Java "` stores java; `GET /api/tags?q=java` filters |
| Tag deletion | Cannot delete while any prompt references it | Attempt returns 409 `CONFLICT`; after prompt removal, deletion succeeds |
| Error contract | Domain exceptions map to HTTP status + `ApiError` JSON | 400 validation, 401 auth/credentials, 403 role, 404 ownership/not-found, 409 rule, 502 Keycloak errors |
| API documentation | SmallRye OpenAPI + Swagger UI | `/q/openapi` and `/q/swagger-ui` available locally; generated source in `target/openapi` |
| Migrations | SQL V1–V6 applied by Flyway with no Hibernate schema generation | Startup creates all categories/folders/tags/prompt/prompt_tag/flyway tables |
| Demo content | Seeders generate owner-linked sample data | A clean dev DB has 100 categories, 100 folders, 100 owner-scoped tags, 100 prompts and roughly 200 tag links |
| Tests | Resource-level integration tests using `@QuarkusTest`, Rest Assured, `@TestSecurity`, Dev Services | `mvnw test` passes: 52 tests across auth, prompt, category, folder, tag suites |

## Acceptance walkthrough

1. Start MySQL/Keycloak and `./mvnw quarkus:dev`.
2. Login as Alice and call `/api/me` to confirm admin role.
3. Create a category and a root folder (optionally with a parent), then create a prompt with optional `folderId`.
4. Verify prompt response gives `folderName=default` if `folderId` was omitted, and all tag names are lowercase + sorted.
5. Create a nested folder and move the prompt into it with `PUT /api/prompts/{id}/move`; verify `folderId` changes while owner stays fixed.
6. Attempt moving a folder underneath itself or a descendant to prove the 409 cycle rejection.
7. Login as Bob and request Alice's prompt or category/tag/folder. Expect 404 for prompt/tag/folder; category mutations return 403 for non-admin users.
8. Delete a tag or category in use and receive `409 CONFLICT`; deleting a removed object's parent collection remains blocked unless empty.

## Non-functional acceptance

- **Ownership isolation:** every prompt, tag, and folder has an owner ID; there are no cross-user folder/tree joins allowed by app services.
- **Validation:** request records have explicit maximum sizes and positive-ID validation; `@Valid` is used at resource boundaries.
- **Transactional mapping:** lazy relations are initialized in service transactions so DTO conversion does not fail outside the persistence session.
- **Reproducibility:** wrapper-based build, local realm import, migration ordering, idempotent demos, containerized test services.
- **API consistency:** resources all use `RestResponse` where status or Location is significant; request/response contracts are OpenAPI generated classes and DTOs.

## Current outcomes / limits for acceptance

- Default behavior intentionally uses 404 rather than leaking whether another user's resource exists.
- Nested tree, owner-scoped tags, and reserved default folders are invariants in service code; database constraints alone do not detect hierarchy cycles or detached non-default folder misuse.
- Single-row editing is used; prompt `version` is an optimistic-lock counter, not a history.
- Admin role changes business visibility, but cross-owner prompt/folder or parent/folder relationships are not legal to preserve tenant consistency.
