# Simplified ER diagram (presentation)

The diagram shows the important collections and ownership boundaries. `folders.parent_id` is a self-reference, so a tree can have any depth. Users are Keycloak accounts and are deliberately **not** modeled as a database table.

```mermaid
erDiagram
    KEYCLOAK_USER ||--o{ FOLDERS : "owns (owner_id)"
    KEYCLOAK_USER ||--o{ TAGS : "owns (owner_id)"
    KEYCLOAK_USER ||--o{ PROMPTS : "owns (owner_id)"

    CATEGORIES ||--o{ PROMPTS : "categorizes"
    FOLDERS ||--o{ PROMPTS : "contains"
    FOLDERS o|--o{ FOLDERS : "optional parent"
    PROMPTS ||--o{ PROMPT_TAGS : "has link rows"
    TAGS ||--o{ PROMPT_TAGS : "is linked from"

    CATEGORIES {
        bigint id PK
        varchar name UK
        datetime created_at
        datetime updated_at
    }
    FOLDERS {
        bigint id PK
        varchar name
        varchar owner_id
        bigint parent_id FK
        datetime created_at
        datetime updated_at
    }
    TAGS {
        bigint id PK
        varchar name
        varchar owner_id
        datetime created_at
        datetime updated_at
    }
    PROMPTS {
        bigint id PK
        varchar title
        text content
        varchar status
        boolean is_favorite
        bigint category_id FK
        varchar owner_id
        bigint folder_id FK
        bigint version
        datetime created_at
        datetime updated_at
    }
    PROMPT_TAGS {
        bigint prompt_id PK_FK
        bigint tag_id PK_FK
    }
    KEYCLOAK_USER {
        string sub "external identity, no SQL table"
    }
```

Multiplicities:

- One category can classify many prompts; every prompt has exactly one category.
- One folder can contain many prompts and many child folders. The parent is optional at the root.
- A prompt belongs to one folder, and every prompt belongs to exactly one owner (`owner_id`).
- Tags and prompts are linked through the join table `prompt_tags`. That is the only n:m relationship.
- `KEYCLOAK_USER` is shown for comprehension only; `owner_id` is a plain string with no database foreign key.

For exact SQL types, indexes and uniqueness rules see [schema.dbml](schema.dbml) and [database.md](README.md).
