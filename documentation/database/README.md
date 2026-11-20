# Database guide

## 1. Sources of truth

The authoritative schema is the sequence of SQL files in `src/main/resources/db/migration`:

- `V1__create_base_tables.sql` — categories, tags, prompts, and the initial join table.
- `V2__add_favorite_to_prompts.sql` — favorite flag.
- `V3__add_owner_to_prompts.sql` — prompt ownership, nullable at first, plus the owner index.
- `V4__owner_id_not_null.sql` — backfills and makes prompt ownership mandatory.
- `V5__add_owner_to_tags.sql` — per-owner tags and per-owner tag-name uniqueness.
- `V6__add_folders.sql` — user-owned folder trees and the mandatory prompt-folder relation.

Flyway runs at application startup. Hibernate does not create or alter tables (`quarkus.hibernate-orm.schema-management.strategy=none`). Never edit a migration that has already run in an environment; add the next version.

## 2. Physical tables

### `categories`

Administrators classify prompts with a shared, non-user-scoped list.

| Column | Type | Null | Purpose |
|---|---|---|---|
| `id` | BIGINT, auto-increment | no | Surrogate primary key |
| `name` | VARCHAR(100) | no | Category label, globally unique (`uq_categories_name`) |
| `created_at` | DATETIME(6) | no | Creation timestamp |
| `updated_at` | DATETIME(6) | no | Last-update timestamp |

Relation: `categories.id → prompts.category_id` (one category to many prompts). The `Prompt` entity maps it with a lazy `@ManyToOne`. The foreign key does not cascade deletion: the application rejects deleting a category that is still referenced.

### `prompts`

The main content table. It stores prompt text, lifecycle metadata, ownership, and placement.

| Column | Type | Null | Purpose |
|---|---|---|---|
| `id` | BIGINT, auto-increment | no | Surrogate primary key |
| `title` | VARCHAR(200) | no | Display title |
| `content` | TEXT | no | Actual prompt body |
| `status` | VARCHAR(20) | no | `DRAFT`, `PUBLISHED`, or `ARCHIVED`; default in the Java entity is `DRAFT` |
| `category_id` | BIGINT | no | Required FK to `categories.id` |
| `is_favorite` | BOOLEAN | no | Favorite flag, default `false` (added in V2) |
| `owner_id` | VARCHAR(36) | no | Keycloak subject that owns the prompt (V3/V4) |
| `folder_id` | BIGINT | no | Required FK to `folders.id` (V6) |
| `version` | BIGINT | no | Optimistic-lock value, default `0` |
| `created_at` | DATETIME(6) | no | Creation timestamp |
| `updated_at` | DATETIME(6) | no | Update timestamp |

Indexes: `idx_prompts_owner(owner_id)` and `idx_prompts_folder(folder_id)`. `owner_id` is **not** a database foreign key: users are deliberately kept in Keycloak, not mirrored in a `users` table. `folder_id`, by contrast, is an actual SQL foreign key.

`version` is an internal locking value, **not** a version-history feature. Today one prompt row is edited in place; there is no separate prompt-version table.

### `folders`

User-owned, optional hierarchy. Every folder ultimately sits in one owner's tree. A root folder has `parent_id = NULL`.

| Column | Type | Null | Purpose |
|---|---|---|---|
| `id` | BIGINT, auto-increment | no | Surrogate primary key |
| `name` | VARCHAR(100) | no | Folder name, unique per owner |
| `owner_id` | VARCHAR(36) | no | Keycloak subject of the owner |
| `parent_id` | BIGINT | **yes** | Optional self FK to `folders.id` |
| `created_at` | DATETIME(6) | no | Creation timestamp |
| `updated_at` | DATETIME(6) | no | Update timestamp |

The composite unique constraint `uq_folders_owner_name(owner_id, name)` prevents duplicate names for one owner. Indexes include owner and parent. The service (not a database trigger) resolves the reserved root `default` folder and prevents moving a folder under itself or a descendant.

Foreign keys:

```text
folders.parent_id → folders.id
prompts.folder_id → folders.id
```

Deleting a folder containing prompts or children fails with `409 CONFLICT`; the reserved default folder is also protected.

### `tags`

Tags are user-scoped labels, not a global namespace.

| Column | Type | Null | Purpose |
|---|---|---|---|
| `id` | BIGINT, auto-increment | no | Surrogate primary key |
| `name` | VARCHAR(50) | no | Normalized lowercase label |
| `owner_id` | VARCHAR(36) | no | Keycloak subject of the owner |
| `created_at` | DATETIME(6) | no | Creation timestamp |
| `updated_at` | DATETIME(6) | no | Update timestamp |

Uniqueness is `uq_tags_owner_name(owner_id, name)`. Alice and Bob can therefore each own an identically named `java` tag. The service filters list/search by the caller's ID unless the caller is an admin. Deleting a tag that is still attached to prompts is rejected with 409.

V5 explicitly backfills older tags: a tag used by multiple owners is split into one owned copy per owner, and `prompt_tags` is rewritten to the copy belonging to the prompt owner.

### `prompt_tags`

The join table for prompts and tags.

| Column | Type | Null | Purpose |
|---|---|---|---|
| `prompt_id` | BIGINT | no | FK to `prompts.id`, cascades on prompt deletion |
| `tag_id` | BIGINT | no | FK to `tags.id`, cascades on tag deletion |

Its PK is `(prompt_id, tag_id)`, so the same tag can occur only once on the same prompt. The two cascade rules remove join rows when their endpoints disappear; the API first rejects tag deletion while the tag is in use. JPA builds and manages these rows through `Prompt.tags`; the join table has no entity and no owner column.

### `flyway_schema_history`

Flyway maintains this internal bookkeeping table. Typical columns include `installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, and `success`. It is not an application entity and should not be edited by hand during normal development. Use it to diagnose failed or out-of-order migrations.

## 3. Logical relationships

```text
categories (1) ─────< (many) prompts
folders   (0..1 parent) ─────< (many child folders)
folders   (1) ─────< (many) prompts
prompts   (many) >───< (many) tags  [via prompt_tags]
Keycloak subject ── owner_id ── folders, prompts, tags (no SQL FK)
```

Be careful to distinguish:

- **Category vs folder:** categories are global classifications with admin-only writes; folders are personal organization with self-nesting and owner-scoped CRUD.
- **Owner relationship:** prompts are never shared across owner folders. Admin bypasses access checks, but cross-owner prompt/folder assignment is not allowed.
- **Tag ownership vs prompt-tag linkage:** a prompt links to its owner's tags; two people can use the same text without sharing a tag entity.

## 4. Timestamps, IDs, and enums

- IDs are `BIGINT AUTO_INCREMENT` and map to `BaseEntity.id` with `GenerationType.IDENTITY`.
- Entities expose `createdAt` and `updatedAt`, mapped from `created_at` and `updated_at` (`DATETIME(6)`). Hibernate supplies values through `@CreationTimestamp` and `@UpdateTimestamp`.
- `Prompt.status` is stored as text, never as an ordinal; changing the enum order therefore cannot rewrite the meaning of stored rows.
- Prompt fields `favorite`/`is_favorite` differ by naming convention only in Java vs SQL.

## 5. Seed data vs migrations

Seeders create development demonstration rows (50 Keycloak-linked users, 100 categories, 100 folders, 100 owner-scoped tags, and 100 prompts, roughly 200 tag links). They use the ORM and repositories, not ad-hoc SQL. They are enabled by `app.seed.enabled` and run in startup order:

| Order | Seeder | Effect |
|---:|---|---|
| 10 | `CategorySeeder` | 100 category names, including Coding / Writing / Marketing |
| 15 | `FolderSeeder` | `default` plus one nested `folder-01` for each of 50 users |
| 20 | `TagSeeder` | `tag-01` and `tag-02` for each of 50 users |
| 30 | `PromptSeeder` | fills missing generated prompt titles to 100 total, two per user |

If an existing database is still partially populated, actual totals can exceed the target because old or custom records are not deleted. Migrations, not seeders, are responsible for permanent schema and required structural data.

## 6. Files in this folder

- [schema.dbml](schema.dbml): DBML source for the complete logical schema, relationships, keys, and index names.
- [er-diagram.md](er-diagram.md): presentation-sized Mermaid ER diagram.
- This guide: narrative explanation, invariants, migration and seeding workflows.
